package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/locataires")
@CrossOrigin(origins = "*")
public class LocataireProxyController {

    private final RestTemplate restTemplate;
    private final String rentReceiptBaseUrl;

    public LocataireProxyController(
            @Value("${services.rentReceiptBaseUrl}") String rentReceiptBaseUrl
    ) {
        this.restTemplate = new RestTemplate();
        this.rentReceiptBaseUrl = rentReceiptBaseUrl;
    }

    @GetMapping
    public ResponseEntity<String> getAllLocataires(
            @RequestHeader(value = "Authorization", required = false) String auth) {

        String url = rentReceiptBaseUrl + "/locataires";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (auth != null) headers.set("Authorization", auth);

        try {
            ResponseEntity<String> upstream = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ResponseEntity
                    .status(upstream.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(upstream.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\":\"Rent-receipt service unavailable\"}");
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        try {
            ResponseEntity<String> upstream = restTemplate.getForEntity(
                    rentReceiptBaseUrl + "/rent-receipt/ping", String.class);
            return ResponseEntity.status(upstream.getStatusCode()).body(upstream.getBody());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("rent-receipt service down");
        }
    }
}
