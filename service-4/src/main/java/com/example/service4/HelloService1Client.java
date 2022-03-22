package com.example.service4;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class HelloService1Client {

  private final WebClient client;

  // Spring Boot auto-configures a `WebClient.Builder` instance with nice defaults and customizations.
  // We can use it to create a dedicated `WebClient` for our component.
  public HelloService1Client(WebClient.Builder builder, ReactiveClientRegistrationRepository clientRegistrations) {
    InMemoryReactiveOAuth2AuthorizedClientService authorizedClientService = new InMemoryReactiveOAuth2AuthorizedClientService(
        clientRegistrations
    );
    AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
        clientRegistrations,
        authorizedClientService
    );

    manager.setContextAttributesMapper(contextAttributesMapper("test", "test"));
    ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider delegatingProvider = new ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider(
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder().password().build(),
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder().refreshToken().build(),
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder().password().build()
    );
    manager.setAuthorizedClientProvider(delegatingProvider);
    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(
            manager
        );
    oauth.setDefaultClientRegistrationId("keycloak");
    ReactiveOAuth2AuthorizationFailureHandler authorizationFailureHandler = new RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler(
        (clientRegistrationId, principal, attributes) -> authorizedClientService.removeAuthorizedClient(clientRegistrationId, principal.getName())
    );
    oauth.setAuthorizationFailureHandler(authorizationFailureHandler);
    this.client = builder.baseUrl("http://localhost:8111").filter(oauth).build();
  }

  private Function<OAuth2AuthorizeRequest, Mono<Map<String, Object>>> contextAttributesMapper(final String username,
          final String password) {
    return authorizeRequest -> {
      final Map<String, Object> contextAttributes = new HashMap<>();
      log.debug("getting access token for {}", username);
      contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username);
      contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password);
      return Mono.just(contextAttributes);
    };
  }

  public Mono<String> getMessage() {
    return this.client.get().uri("/example/hello").accept(APPLICATION_JSON)
        .retrieve()
        .bodyToMono(Greeting.class)
        .map(Greeting::getMessage);
  }

}
