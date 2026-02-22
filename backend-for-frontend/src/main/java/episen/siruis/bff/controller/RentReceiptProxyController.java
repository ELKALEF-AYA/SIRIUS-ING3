package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/rent-receipt")
public class RentReceiptProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String rentReceiptBaseUrl;

    public RentReceiptProxyController(
            @Value("${services.rentReceiptBaseUrl}") String rentReceiptBaseUrl
    ) {
        this.rentReceiptBaseUrl = rentReceiptBaseUrl;
    }

    private ResponseEntity<String> clean(ResponseEntity<String> upstream) {
        return ResponseEntity
                .status(upstream.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstream.getBody());
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateQuittance(
            @RequestParam("locataireId") Long locataireId,
            @RequestParam("periode") String periode,
            @RequestParam("charges") BigDecimal charges
    ) {
        String url = rentReceiptBaseUrl + "/rent-receipt/generate"
                + "?locataireId=" + locataireId
                + "&periode=" + periode
                + "&charges=" + charges;
        try {
            return clean(restTemplate.postForEntity(url, null, String.class));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\":\"Rent-receipt service unavailable\"}");
        }
    }

    @PostMapping("/download")
    public ResponseEntity<String> downloadQuittance(
            @RequestParam("quittanceId") Long quittanceId
    ) {
        String url = rentReceiptBaseUrl + "/rent-receipt/download?quittanceId=" + quittanceId;
        try {
            return clean(restTemplate.postForEntity(url, null, String.class));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\":\"Rent-receipt service unavailable\"}");
        }
    }

    @GetMapping("/preview")
    public ResponseEntity<String> previewQuittance(
            @RequestParam("locataireId") Long locataireId,
            @RequestParam("periode") String periode,
            @RequestParam("charges") BigDecimal charges
    ) {
        String url = rentReceiptBaseUrl + "/rent-receipt/preview"
                + "?locataireId=" + locataireId
                + "&periode=" + periode
                + "&charges=" + charges;
        try {
            return clean(restTemplate.getForEntity(url, String.class));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\":\"Rent-receipt service unavailable\"}");
        }
    }

    @GetMapping("/client/{locataireId}")
    public ResponseEntity<String> getQuittancesByClient(
            @PathVariable("locataireId") Long locataireId
    ) {
        String url = rentReceiptBaseUrl + "/rent-receipt/client/" + locataireId;
        try {
            return clean(restTemplate.getForEntity(url, String.class));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\":\"Rent-receipt service unavailable\"}");
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        try {
            return clean(restTemplate.getForEntity(rentReceiptBaseUrl + "/rent-receipt/ping", String.class));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("rent-receipt service down");
        }
    }
}
