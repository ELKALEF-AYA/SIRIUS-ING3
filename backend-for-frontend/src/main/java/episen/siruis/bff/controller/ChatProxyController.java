package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String chatBaseUrl;

    public ChatProxyController(@Value("${services.chatBaseUrl:http://localhost:8081}") String chatBaseUrl) {
        this.chatBaseUrl = chatBaseUrl;
    }

    /**
     * Proxy GET pour historique
     */
    @GetMapping("/history/{conversationId}")
    public ResponseEntity<?> getHistory(@PathVariable Long conversationId) {
        String url = chatBaseUrl + "/api/chat/history/" + conversationId;
        return restTemplate.getForEntity(url, List.class);
    }

    /**
     * Proxy health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        String url = chatBaseUrl + "/api/chat/health";
        return restTemplate.getForEntity(url, String.class);
    }


}