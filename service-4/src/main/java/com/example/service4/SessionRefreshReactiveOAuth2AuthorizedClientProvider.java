/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.service4;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.OAuth2PasswordGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactivePasswordTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;

/**
 * An implementation of a {@link ReactiveOAuth2AuthorizedClientProvider} for the
 * {@link AuthorizationGrantType#PASSWORD password} grant.
 *
 * @author Joe Grandja
 * @see ReactiveOAuth2AuthorizedClientProvider
 * @see WebClientReactivePasswordTokenResponseClient
 * @since 5.2
 */
@Slf4j public final class SessionRefreshReactiveOAuth2AuthorizedClientProvider
		implements ReactiveOAuth2AuthorizedClientProvider {

	private ReactiveOAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> accessTokenResponseClient = new WebClientReactivePasswordTokenResponseClient();

	private Duration clockSkew = Duration.ofSeconds(60);

	private Clock clock = Clock.systemUTC();

	@Override public Mono<OAuth2AuthorizedClient> authorize(OAuth2AuthorizationContext context) {
		Assert.notNull(context, "context cannot be null");
		ClientRegistration clientRegistration = context.getClientRegistration();
		OAuth2AuthorizedClient authorizedClient = context.getAuthorizedClient();
		log.info("EmsysRenewSessionProvider");
		if (!AuthorizationGrantType.PASSWORD.equals(clientRegistration.getAuthorizationGrantType())) {
			return Mono.empty();
		}
		String username = context.getAttribute(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME);
		String password = context.getAttribute(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME);
		if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
			return Mono.empty();
		}
		if (authorizedClient != null && !hasTokenExpired(authorizedClient.getAccessToken()) && authorizedClient.getRefreshToken()!=null) {
			log.info("Session availabel, refresh active. ");
			return Mono.empty();
		}
		OAuth2PasswordGrantRequest passwordGrantRequest = new OAuth2PasswordGrantRequest(clientRegistration, username,
				password);
		log.info("Authorizing: {}", username);
		return Mono.just(passwordGrantRequest)
				.flatMap(this.accessTokenResponseClient::getTokenResponse)
				.onErrorMap(OAuth2AuthorizationException.class,
						(e) -> new ClientAuthorizationException(e.getError(), clientRegistration.getRegistrationId(),
								e))
				.map((tokenResponse) -> new OAuth2AuthorizedClient(clientRegistration, context.getPrincipal().getName(),
						tokenResponse.getAccessToken(), tokenResponse.getRefreshToken()));
	}

	private boolean hasTokenExpired(OAuth2Token token) {
		return this.clock.instant().isAfter(token.getExpiresAt().minus(this.clockSkew));
	}

	public void setAccessTokenResponseClient(
			ReactiveOAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> accessTokenResponseClient) {
		Assert.notNull(accessTokenResponseClient, "accessTokenResponseClient cannot be null");
		this.accessTokenResponseClient = accessTokenResponseClient;
	}

	public void setClockSkew(Duration clockSkew) {
		Assert.notNull(clockSkew, "clockSkew cannot be null");
		Assert.isTrue(clockSkew.getSeconds() >= 0, "clockSkew must be >= 0");
		this.clockSkew = clockSkew;
	}

	public void setClock(Clock clock) {
		Assert.notNull(clock, "clock cannot be null");
		this.clock = clock;
	}

}
