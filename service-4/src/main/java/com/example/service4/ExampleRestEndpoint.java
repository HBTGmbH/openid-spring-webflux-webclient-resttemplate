package com.example.service4;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/example")
@RequiredArgsConstructor
public class ExampleRestEndpoint {

  private final HelloService1Client helloService1Client;

  @GetMapping(path = "/hello")
  public Mono<String> sayHello() {
    return helloService1Client.getMessage();
  }

}
