package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/notifications")
public class NotificationProxyController {

    private final WebClient webClient;

    public NotificationProxyController(
            @Value("${services.notificationBaseUrl}") String notifBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(notifBaseUrl)
                .build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return webClient.get()
                .uri("/api/notifications/stream")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToFlux(String.class);
    }
}