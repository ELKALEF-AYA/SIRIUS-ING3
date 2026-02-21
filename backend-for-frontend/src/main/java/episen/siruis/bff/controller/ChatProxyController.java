package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173")
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

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestHeader("Authorization") String auth) {
        HttpEntity<?> entity = new HttpEntity<>(forwardAuth(auth));
        return restTemplate.exchange(chatBaseUrl + "/api/chat/conversations", HttpMethod.GET, entity, Object.class);
    }

    @GetMapping("/my-conversations")
    public ResponseEntity<?> getMyConversations(@RequestHeader("Authorization") String auth) {
        HttpEntity<?> entity = new HttpEntity<>(forwardAuth(auth));
        return restTemplate.exchange(chatBaseUrl + "/api/chat/my-conversations", HttpMethod.GET, entity, Object.class);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable("id") Long id, @RequestHeader("Authorization") String auth) {
        HttpEntity<?> entity = new HttpEntity<>(forwardAuth(auth));
        return restTemplate.exchange(chatBaseUrl + "/api/chat/conversations/" + id + "/messages", HttpMethod.GET, entity, Object.class);
    }

    @PostMapping("/conversations/start")
    public ResponseEntity<?> startConversation(@RequestHeader("Authorization") String auth, @RequestBody Map<String, Long> body) {
        HttpEntity<?> entity = new HttpEntity<>(body, forwardAuth(auth));
        return restTemplate.exchange(chatBaseUrl + "/api/chat/conversations/start", HttpMethod.POST, entity, Object.class);
    }

    @GetMapping("/clients")
    public ResponseEntity<?> getClients(@RequestHeader("Authorization") String auth) {
        HttpEntity<?> entity = new HttpEntity<>(forwardAuth(auth));
        return restTemplate.exchange(chatBaseUrl + "/api/chat/clients", HttpMethod.GET, entity, Object.class);
    }
}