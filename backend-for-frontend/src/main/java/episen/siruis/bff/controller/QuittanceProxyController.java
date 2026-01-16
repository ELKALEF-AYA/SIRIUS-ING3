package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/quittances")
public class QuittanceProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String rentReceiptBaseUrl;

    public QuittanceProxyController(
            @Value("${services.rentReceiptBaseUrl}") String rentReceiptBaseUrl
    ) {
        this.rentReceiptBaseUrl = rentReceiptBaseUrl;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateQuittance(
            @RequestParam Long locataireId,
            @RequestParam String periode
    ) {
        String url = rentReceiptBaseUrl
                + "/quittances/generate"
                + "?locataireId=" + locataireId
                + "&periode=" + periode;

        ResponseEntity<String> upstream =
                restTemplate.postForEntity(url, null, String.class);

        return ResponseEntity
                .status(upstream.getStatusCode())
                .body(upstream.getBody());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Void> downloadQuittance(@PathVariable Long id) {

        String url = rentReceiptBaseUrl + "/quittances/" + id + "/download";

        ResponseEntity<Void> upstream =
                restTemplate.exchange(url, HttpMethod.GET, null, Void.class);

        return ResponseEntity
                .status(upstream.getStatusCode())
                .headers(upstream.getHeaders())
                .build();
    }
}
