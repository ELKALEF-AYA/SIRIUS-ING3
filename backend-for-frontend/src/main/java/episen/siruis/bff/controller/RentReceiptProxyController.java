package episen.siruis.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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


    @PostMapping("/generate")
    public ResponseEntity<String> generateQuittance(
            @RequestParam("locataireId") Long locataireId,
            @RequestParam("periode") String periode,
            @RequestParam("charges") BigDecimal charges
    ) {

        String url = rentReceiptBaseUrl
                + "/rent-receipt/generate"
                + "?locataireId=" + locataireId
                + "&periode=" + periode
                + "&charges=" + charges;

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, null, String.class);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response.getBody());
    }


    @PostMapping("/download")
    public ResponseEntity<String> downloadQuittance(
            @RequestParam("quittanceId") Long quittanceId
    ) {

        String url = rentReceiptBaseUrl
                + "/rent-receipt/download"
                + "?quittanceId=" + quittanceId;

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, null, String.class);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response.getBody());
    }


    @GetMapping("/preview")
    public ResponseEntity<String> previewQuittance(
            @RequestParam("locataireId") Long locataireId,
            @RequestParam("periode") String periode,
            @RequestParam("charges") BigDecimal charges
    ) {

        String url = rentReceiptBaseUrl
                + "/rent-receipt/preview"
                + "?locataireId=" + locataireId
                + "&periode=" + periode
                + "&charges=" + charges;

        ResponseEntity<String> response =
                restTemplate.getForEntity(url, String.class);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response.getBody());
    }


    @GetMapping("/client/{locataireId}")
    public ResponseEntity<String> getQuittancesByClient(
            @PathVariable("locataireId") Long locataireId
    ) {

        String url = rentReceiptBaseUrl
                + "/rent-receipt/client/" + locataireId;

        ResponseEntity<String> response =
                restTemplate.getForEntity(url, String.class);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response.getBody());
    }


    @GetMapping("/ping")
    public ResponseEntity<String> ping() {

        String url = rentReceiptBaseUrl + "/rent-receipt/ping";

        ResponseEntity<String> response =
                restTemplate.getForEntity(url, String.class);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response.getBody());
    }
}
