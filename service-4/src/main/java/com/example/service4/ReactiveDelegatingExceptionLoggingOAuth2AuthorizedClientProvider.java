package com.example.service4;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.*;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j public final class ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider
		implements ReactiveOAuth2AuthorizedClientProvider {

	private final List<ReactiveOAuth2AuthorizedClientProvider> authorizedClientProviders;

	public ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider(
			ReactiveOAuth2AuthorizedClientProvider... authorizedClientProviders) {
		Assert.notEmpty(authorizedClientProviders, "authorizedClientProviders cannot be empty");
		this.authorizedClientProviders = Collections.unmodifiableList(Arrays.asList(authorizedClientProviders));
	}

	public ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider(
			List<ReactiveOAuth2AuthorizedClientProvider> authorizedClientProviders) {
		Assert.notEmpty(authorizedClientProviders, "authorizedClientProviders cannot be empty");
		this.authorizedClientProviders = Collections.unmodifiableList(new ArrayList<>(authorizedClientProviders));
	}

	@Override @Nullable public Mono<OAuth2AuthorizedClient> authorize(OAuth2AuthorizationContext context) {
		Assert.notNull(context, "context cannot be null");
		for (ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider : this.authorizedClientProviders) {
			try {

				//TODO FIX ME FUK'N MONO !!!
				Mono<OAuth2AuthorizedClient> oauth2AuthorizedClient = authorizedClientProvider.authorize(context);
				oauth2AuthorizedClient.subscribe();
				Optional<OAuth2AuthorizedClient> oAuth2AuthorizedClient = oauth2AuthorizedClient.blockOptional();
				if (oAuth2AuthorizedClient.isPresent()) {
					return oauth2AuthorizedClient;
				}
			} catch (ClientAuthorizationException e) {
				log.info("Caught ClientAuthorizationException. Move to next OAuth2AuthorizedClientProvider.", e);
				// Context reset, damit kein AuthorizedClient mehr im Context befindlich und damit kein veralteter refresh token
				// oder andere veraltete Informationen im Context
				Map<String, Object> attributesMap = context.getAttributes();
				context = OAuth2AuthorizationContext.withClientRegistration(context.getClientRegistration())
						.principal(context.getPrincipal())
						.attributes((attributes) -> attributes.putAll(attributesMap))
						.build();
			}
		}
		return null;
	}

}
