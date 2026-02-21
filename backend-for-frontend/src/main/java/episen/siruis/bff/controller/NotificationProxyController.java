package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications")
public class NotificationProxyController {

    private final WebClient webClient;

    public NotificationProxyController(@Value("${services.notificationBaseUrl}") String notifBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(notifBaseUrl)
                .build();
    }

    @GetMapping(value="/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token manquant.");
        }

        return webClient.get()
                .uri("/api/notifications/stream")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class);
    }

    @GetMapping("/me")
    public Mono<String> my(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String auth = requireBearer(authorization);

        return webClient.get()
                .uri("/api/notifications/me")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(String.class);
    }

    @GetMapping("/me/unread-count")
    public Mono<String> unreadCount(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String auth = requireBearer(authorization);

        return webClient.get()
                .uri("/api/notifications/me/unread-count")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(String.class);
    }

    @PatchMapping("/{id}/read")
    public Mono<String> markOneRead(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long id
    ) {
        String auth = requireBearer(authorization);

        return webClient.patch()
                .uri("/api/notifications/{id}/read", id)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(String.class);
    }

    @PatchMapping("/me/read-all")
    public Mono<String> markAllRead(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        String auth = requireBearer(authorization);

        return webClient.patch()
                .uri("/api/notifications/me/read-all")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(String.class);
    }

    private String requireBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token manquant ou invalide.");
        }
        return authorization;
    }
}