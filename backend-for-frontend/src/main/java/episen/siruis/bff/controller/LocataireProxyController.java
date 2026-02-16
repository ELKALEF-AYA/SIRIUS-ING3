package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/locataires")
@CrossOrigin(origins = "http://localhost:5173")
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
    public ResponseEntity<String> getAllLocataires() {

        String url = rentReceiptBaseUrl + "/locataires";

        return restTemplate.getForEntity(url, String.class);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {

        String url = rentReceiptBaseUrl + "/rent-receipt/ping";

        return restTemplate.getForEntity(url, String.class);
    }
}
