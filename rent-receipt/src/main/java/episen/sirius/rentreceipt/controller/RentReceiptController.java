package episen.sirius.rentreceipt.controller;

import episen.sirius.rentreceipt.model.RentReceipt;
import episen.sirius.rentreceipt.service.RentReceiptService;
import episen.sirius.rentreceipt.service.MinioStorageService;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quittances")
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
            @RequestParam("periode") String periode
    ) {
        return rentReceiptService.creerQuittance(locataireId, periode);
    }

    @GetMapping("/{id}/download")
        public ResponseEntity<Void> downloadQuittance(@PathVariable("id") Long id) {


            RentReceipt rentReceipt = rentReceiptService.findById(id);
        String presignedUrl =
                minioStorageService.generatePresignedUrl(rentReceipt.getFilePath());

        return ResponseEntity
                .status(302)
                .header("Location", presignedUrl)
                .build();
    }
    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }

}

