package episen.sirius.rentreceipt.controller;
import java.math.BigDecimal;
import java.util.List;

import episen.sirius.rentreceipt.model.RentReceipt;
import episen.sirius.rentreceipt.service.RentReceiptService;
import episen.sirius.rentreceipt.service.MinioStorageService;
import org.springframework.http.ResponseEntity;
import episen.sirius.rentreceipt.dto.RentReceiptPreviewDTO;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rent-receipt")
public class RentReceiptController {

    private final RentReceiptService rentReceiptService;
    private final MinioStorageService minioStorageService;

    public RentReceiptController(
            RentReceiptService rentReceiptService,
            MinioStorageService minioStorageService
    ) {
        this.rentReceiptService = rentReceiptService;
        this.minioStorageService = minioStorageService;
    }

    @PostMapping("/generate")
    public RentReceipt creerQuittance(
            @RequestParam("locataireId") Long locataireId,
            @RequestParam("periode") String periode,
            @RequestParam("charges") BigDecimal charges
    ) {
        return rentReceiptService.creerQuittance(locataireId, periode, charges);
    }


    @PostMapping("/download")
    public ResponseEntity<String> downloadQuittance(
            @RequestParam("quittanceId") Long quittanceId
    ) {
        RentReceipt rentReceipt = rentReceiptService.findById(quittanceId);

        String presignedUrl =
                minioStorageService.generatePresignedUrl(
                        rentReceipt.getFilePath()
                );

        return ResponseEntity.ok(presignedUrl);
    }


    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }
    @GetMapping("/preview")
    public RentReceiptPreviewDTO previewQuittance(
            @RequestParam("locataireId") Long locataireId,
            @RequestParam("periode") String periode,
            @RequestParam("charges") BigDecimal charges
    ) {
        return rentReceiptService.previewQuittance(locataireId, periode, charges);
    }

    @GetMapping("/client/{locataireId}")
    public List<RentReceipt> getQuittancesClient(
            @PathVariable("locataireId") Long locataireId
    ) {
        return rentReceiptService.getQuittancesByLocataire(locataireId);
    }


}

