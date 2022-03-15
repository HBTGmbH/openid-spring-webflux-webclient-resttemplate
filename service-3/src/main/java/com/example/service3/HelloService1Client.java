package com.example.service3;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

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

  public String getMessage() {
    return restTemplate.getForObject("/example/hello", Greeting.class).getMessage();
  }

}
