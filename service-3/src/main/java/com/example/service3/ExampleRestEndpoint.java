package com.example.service3;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/example")
@RequiredArgsConstructor
public class ExampleRestEndpoint {

  private final HelloService1Client helloService1Client;

  @GetMapping(path = "/hello")
  public String sayHello() {
    return helloService1Client.getMessage();
  }

}
