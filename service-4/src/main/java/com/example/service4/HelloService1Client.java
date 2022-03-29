package com.example.service4;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.retry.Retry;

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
    ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider delegatingProvider =
            new ReactiveDelegatingExceptionLoggingOAuth2AuthorizedClientProvider(
				new PasswordReactiveOAuth2AuthorizedClientProvider(),
				new RefreshTokenReactiveOAuth2AuthorizedClientProvider(),
				new SessionRefreshReactiveOAuth2AuthorizedClientProvider()
   		 );
    manager.setAuthorizedClientProvider(delegatingProvider);
    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(
            manager
        );
      oauth.setDefaultClientRegistrationId("massnahmen");

//    Retry customStrategy = Retry.from(companion -> companion.handle((retrySignal, sink) -> {
//      Context ctx = sink.currentContext();
//      boolean sendErrorToSink = true;
//      if(retrySignal.failure() instanceof ClientAuthorizationException) {
//        int rl = ctx.getOrDefault("retriesLeft", 1);
//        if (rl == 1) {
//          log.info("Got ClientAuthorizationException -> Retry execution!", retrySignal.failure());
//          sendErrorToSink = false;
//          sink.next(Context.of(
//              "retriesLeft", 0
//          ));
//        }
//      }
//      if(sendErrorToSink) {
//        sink.error(retrySignal.failure());
//      }
//    }));

    this.client = builder
            .baseUrl("http://localhost:8111")
//            .filter((request, next) -> next.exchange(request).retryWhen(customStrategy))
            .filter(oauth)
            .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
              StringBuilder sb = new StringBuilder("Request: ");
              sb.append(clientRequest.method()).append(" ").append(clientRequest.url());
              log.info(sb.toString());
              return Mono.just(clientRequest);
            }))
            .build();
  }

  private Function<OAuth2AuthorizeRequest, Mono<Map<String, Object>>> contextAttributesMapper(final String username,
          final String password) {
    return authorizeRequest -> {
      final Map<String, Object> contextAttributes = new HashMap<>();
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
