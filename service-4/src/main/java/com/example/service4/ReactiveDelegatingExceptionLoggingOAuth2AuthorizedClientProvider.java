package com.example.service4;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.*;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
		AtomicReference<OAuth2AuthorizationContext> authContext = new AtomicReference<>(context);
		AtomicInteger count = new AtomicInteger();
		return Flux.fromIterable(this.authorizedClientProviders)
				.concatMap(reactiveOAuth2AuthorizedClientProvider -> Mono.defer(
						() -> reactiveOAuth2AuthorizedClientProvider.authorize(context)))
				.filter(Objects::nonNull)
				.onErrorContinue((throwable, o) -> {
					reAuthorize(throwable, authContext, count.getAndIncrement());
					log.info("Context: {}", authContext.get().getAuthorizedClient().getRefreshToken());
					log.info(throwable.getMessage());
				})
				.next();
	}

	private Throwable reAuthorize(Throwable e, AtomicReference<OAuth2AuthorizationContext> authContext, int count) {
		log.info("Caught ClientAuthorizationException. Move to next OAuth2AuthorizedClientProvider.");
		// Context reset, damit kein AuthorizedClient mehr im Context befindlich und damit kein veralteter refresh token
		// oder andere veraltete Informationen im Context
		OAuth2AuthorizationContext context = authContext.get();
		Map<String, Object> attributesMap = context.getAttributes();

		authContext.set(OAuth2AuthorizationContext.withAuthorizedClient(
						new OAuth2AuthorizedClient(authContext.get().getClientRegistration(),
								authContext.get().getPrincipal().getName(),
								authContext.get().getAuthorizedClient().getAccessToken(), null))
				.principal(context.getPrincipal())
				.attributes((attributes) -> attributes.putAll(attributesMap))
				.build());

		return e;
	}

}
