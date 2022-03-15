package com.example.service3;

import static java.util.concurrent.TimeUnit.SECONDS;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledGreeter {

  private final HelloService1Client helloService1Client;

  @Scheduled(fixedRate = 30, timeUnit = SECONDS)
  public void doGreeting() {
    System.out.println(helloService1Client.getMessage());
  }

}
