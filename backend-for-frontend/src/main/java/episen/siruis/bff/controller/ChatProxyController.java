package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;

import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatProxyController {

    private final RestTemplate restTemplate;
    private final String chatBaseUrl;

    public ChatProxyController(@Value("${services.chatBaseUrl:http://localhost:8083}") String chatBaseUrl) {
        this.chatBaseUrl = chatBaseUrl;
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });
    }

    private HttpHeaders forwardAuth(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authHeader != null) headers.set("Authorization", authHeader);
        return headers;
    }

    private ResponseEntity<?> clean(ResponseEntity<?> upstream) {
        return ResponseEntity
                .status(upstream.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstream.getBody());
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(forwardAuth(auth));
            return clean(restTemplate.exchange(chatBaseUrl + "/api/chat/conversations", HttpMethod.GET, entity, Object.class));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("error", "Chat service unavailable"));
        }
    }

    @GetMapping("/my-conversations")
    public ResponseEntity<?> getMyConversations(@RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(forwardAuth(auth));
            return clean(restTemplate.exchange(chatBaseUrl + "/api/chat/my-conversations", HttpMethod.GET, entity, Object.class));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("error", "Chat service unavailable"));
        }
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable("id") Long id, @RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(forwardAuth(auth));
            return clean(restTemplate.exchange(chatBaseUrl + "/api/chat/conversations/" + id + "/messages", HttpMethod.GET, entity, Object.class));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("error", "Chat service unavailable"));
        }
    }

    @PostMapping("/conversations/start")
    public ResponseEntity<?> startConversation(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Map<String, Long> body) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(body, forwardAuth(auth));
            return clean(restTemplate.exchange(chatBaseUrl + "/api/chat/conversations/start", HttpMethod.POST, entity, Object.class));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("error", "Chat service unavailable"));
        }
    }

    @GetMapping("/clients")
    public ResponseEntity<?> getClients(@RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(forwardAuth(auth));
            return clean(restTemplate.exchange(chatBaseUrl + "/api/chat/clients", HttpMethod.GET, entity, Object.class));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("error", "Chat service unavailable"));
        }
    }
}