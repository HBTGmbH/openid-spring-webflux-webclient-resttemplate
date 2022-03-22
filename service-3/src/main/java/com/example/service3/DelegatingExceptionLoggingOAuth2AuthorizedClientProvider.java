package com.example.service3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.util.Assert;

@Slf4j
public final class DelegatingExceptionLoggingOAuth2AuthorizedClientProvider implements OAuth2AuthorizedClientProvider {

  private final List<OAuth2AuthorizedClientProvider> authorizedClientProviders;

  public DelegatingExceptionLoggingOAuth2AuthorizedClientProvider(OAuth2AuthorizedClientProvider... authorizedClientProviders) {
    Assert.notEmpty(authorizedClientProviders, "authorizedClientProviders cannot be empty");
    this.authorizedClientProviders = Collections.unmodifiableList(Arrays.asList(authorizedClientProviders));
  }

  public DelegatingExceptionLoggingOAuth2AuthorizedClientProvider(List<OAuth2AuthorizedClientProvider> authorizedClientProviders) {
    Assert.notEmpty(authorizedClientProviders, "authorizedClientProviders cannot be empty");
    this.authorizedClientProviders = Collections.unmodifiableList(new ArrayList<>(authorizedClientProviders));
  }

  @Override
  @Nullable
  public OAuth2AuthorizedClient authorize(OAuth2AuthorizationContext context) {
    Assert.notNull(context, "context cannot be null");
    for (OAuth2AuthorizedClientProvider authorizedClientProvider : this.authorizedClientProviders) {
      try {
        OAuth2AuthorizedClient oauth2AuthorizedClient = authorizedClientProvider.authorize(context);
        if (oauth2AuthorizedClient != null) {
          return oauth2AuthorizedClient;
        }
      } catch (ClientAuthorizationException e) {
        log.debug("Caught ClientAuthorizationException. Move to next OAuth2AuthorizedClientProvider.", e);
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
