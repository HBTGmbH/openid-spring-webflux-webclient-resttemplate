package com.example.service1;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/example")
public class ExampleRestEndpoint {

  @GetMapping(path = "/hello")
  public Mono<Greeting> sayHello(Authentication authentication) {
    return Mono.just(getData(authentication));
  }

  private Greeting getData(Authentication authentication) {
    if(authentication instanceof JwtAuthenticationToken) {
      JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
      System.out.println(token.getToken().getTokenValue());
      System.out.println(token.getToken().getExpiresAt());
    }
    return new Greeting("hell yeah!");
  }

}
