package com.example.service4;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledGreeter {

    private final HelloServiceDelegatedSessionRefreshClient helloService1Client;
    private final HelloServiceRetryClient helloServiceRetryClient;

    @Scheduled(fixedRate = 15, timeUnit = SECONDS)
    public void doGreeting() {
        log.info("Session Refresh: {}", helloService1Client.getMessage().block(Duration.ofSeconds(10)));
        log.info("On Error Retry: {}", helloServiceRetryClient.getMessage().block(Duration.ofSeconds(10)));
    }

}
