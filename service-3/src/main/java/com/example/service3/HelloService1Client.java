package com.example.service3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Component
@Slf4j
public class HelloService1Client {

  private final RestTemplate restTemplate;

  public HelloService1Client(ClientRegistrationRepository clientRegistrations) {
    final InMemoryOAuth2AuthorizedClientService authorizedClientService = new InMemoryOAuth2AuthorizedClientService(
        clientRegistrations
    );
    final AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
        clientRegistrations,
        authorizedClientService
    );
    manager.setContextAttributesMapper(contextAttributesMapper());
    DelegatingExceptionLoggingOAuth2AuthorizedClientProvider delegatingProvider = new DelegatingExceptionLoggingOAuth2AuthorizedClientProvider(
        OAuth2AuthorizedClientProviderBuilder.builder().password().build(),
        OAuth2AuthorizedClientProviderBuilder.builder().refreshToken().build()
    );
    manager.setAuthorizedClientProvider(delegatingProvider);
    restTemplate = new RestTemplate();
    restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:8111"));
    restTemplate.getInterceptors().add((request, body, execution) -> {
      OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
          .withClientRegistrationId("keycloak")
          .principal("keycloak")
          .build();
      OAuth2AuthorizedClient authorizedClient = manager.authorize(authRequest);
      OAuth2AccessToken accessToken = Objects.requireNonNull(authorizedClient).getAccessToken();

      log.info("Issued: " + accessToken.getIssuedAt() + ", Expires:" + accessToken.getExpiresAt());
      log.info("Scopes: " + accessToken.getScopes());
      log.info("Token: " + accessToken.getTokenValue());

      request.getHeaders().set("Authorization", "Bearer " + accessToken.getTokenValue());

      return execution.execute(request, body);
    });
  }

  private Function<OAuth2AuthorizeRequest,Map<String, Object>> contextAttributesMapper() {
    final String username = "test";
    final String password = "test";
    return authorizeRequest -> {
      final Map<String, Object> contextAttributes = new HashMap<>();
      log.debug("getting access token for {}", username);
      contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username);
      contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password);
      return contextAttributes;
    };
  }

  public String getMessage() {
    return restTemplate.getForObject("/example/hello", Greeting.class).getMessage();
  }

}
