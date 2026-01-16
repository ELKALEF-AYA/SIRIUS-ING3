package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/auth")
public class AuthProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String authBaseUrl;

    public AuthProxyController(@Value("${services.authBaseUrl}") String authBaseUrl) {
        this.authBaseUrl = authBaseUrl;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody String body) {
        String url = authBaseUrl + "/auth/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> upstream =
                restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

        return ResponseEntity
                .status(upstream.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstream.getBody());
    }
}