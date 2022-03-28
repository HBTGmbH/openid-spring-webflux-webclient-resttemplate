package com.example.service4;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.*;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

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
		return Flux.fromIterable(this.authorizedClientProviders)
				.flatMap(reactiveOAuth2AuthorizedClientProvider -> Mono.defer(
						() -> reactiveOAuth2AuthorizedClientProvider.authorize(context)))
				.filter(Objects::nonNull)
				.onErrorMap(throwable -> reAuthorize(throwable, context))
				.next();
	}

	private Throwable reAuthorize(Throwable e, OAuth2AuthorizationContext context) {
		log.info("Caught ClientAuthorizationException. Move to next OAuth2AuthorizedClientProvider.", e);
		// Context reset, damit kein AuthorizedClient mehr im Context befindlich und damit kein veralteter refresh token
		// oder andere veraltete Informationen im Context
		Map<String, Object> attributesMap = context.getAttributes();

		context = OAuth2AuthorizationContext.withClientRegistration(
						context.getClientRegistration())
				.principal(context.getPrincipal())
				.attributes((attributes) -> attributes.putAll(attributesMap))
				.build();
		return e;
	}


}
