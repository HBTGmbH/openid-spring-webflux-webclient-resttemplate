package com.example.service2;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

  public Mono<String> getMessage() {
    return this.client.get().uri("/example/hello").accept(APPLICATION_JSON)
        .retrieve()
        .bodyToMono(Greeting.class)
        .map(Greeting::getMessage);
  }

}
