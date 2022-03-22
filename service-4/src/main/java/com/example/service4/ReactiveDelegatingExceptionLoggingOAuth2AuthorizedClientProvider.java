package com.example.service4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.*;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.*;

public final class ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider
		implements ReactiveOAuth2AuthorizedClientProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(
			ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider.class.getName());
	private final List<ReactiveOAuth2AuthorizedClientProvider> authorizedClientProviders;

	/**
	 * Constructs a {@code DelegatingReactiveOAuth2AuthorizedClientProvider} using the
	 * provided parameters.
	 *
	 * @param authorizedClientProviders a list of
	 *                                  {@link ReactiveOAuth2AuthorizedClientProvider}(s)
	 */
	public ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider(
			ReactiveOAuth2AuthorizedClientProvider... authorizedClientProviders) {
		Assert.notEmpty(authorizedClientProviders, "authorizedClientProviders cannot be empty");
		this.authorizedClientProviders = Collections.unmodifiableList(Arrays.asList(authorizedClientProviders));
	}

	/**
	 * Constructs a {@code DelegatingReactiveOAuth2AuthorizedClientProvider} using the
	 * provided parameters.
	 *
	 * @param authorizedClientProviders a {@code List} of
	 *                                  {@link OAuth2AuthorizedClientProvider}(s)
	 */
	public ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider(
			List<ReactiveOAuth2AuthorizedClientProvider> authorizedClientProviders) {
		Assert.notEmpty(authorizedClientProviders, "authorizedClientProviders cannot be empty");
		this.authorizedClientProviders = Collections.unmodifiableList(new ArrayList<>(authorizedClientProviders));
	}

	@Override public Mono<OAuth2AuthorizedClient> authorize(OAuth2AuthorizationContext context) {
		Assert.notNull(context, "context cannot be null");
		for (ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider : this.authorizedClientProviders) {
			try {
				Mono<OAuth2AuthorizedClient> oauth2AuthorizedClient = authorizedClientProvider.authorize(context);
				if (oauth2AuthorizedClient != null) {
					return oauth2AuthorizedClient;
				}
			} catch (ClientAuthorizationException e) {
				LOGGER.debug("Caught ClientAuthorizationException. Move to next OAuth2AuthorizedClientProvider.", e);
				// Context reset, damit kein AuthorizedClient mehr im Context befindlich und damit kein veralteter refresh token
				// oder andere veraltete Informationen im Context
				Map<String, Object> attributesMap = context.getAttributes();
				context = OAuth2AuthorizationContext
						.withClientRegistration(context.getClientRegistration())
						.principal(context.getPrincipal())
						.attributes((attributes) -> attributes.putAll(attributesMap))
						.build();
			}
		}
		return null;
	}


}
