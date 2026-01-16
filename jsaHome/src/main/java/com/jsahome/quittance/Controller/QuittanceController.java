package com.jsahome.quittance.controller;

import com.jsahome.quittance.model.Quittance;
import com.jsahome.quittance.service.QuittanceService;
import com.jsahome.quittance.service.MinioStorageService;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/quittances")
public class QuittanceController {

    private final QuittanceService quittanceService;
    private final MinioStorageService minioStorageService;

    public QuittanceController(
            QuittanceService quittanceService,
            MinioStorageService minioStorageService
    ) {
        this.quittanceService = quittanceService;
        this.minioStorageService = minioStorageService;
    }

    @PostMapping("/generate")
    public Quittance creerQuittance(
            @RequestParam Long locataireId,
            @RequestParam String periode
    ) {
        return quittanceService.creerQuittance(locataireId, periode);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Void> downloadQuittance(@PathVariable Long id) {

        Quittance quittance = quittanceService.findById(id);
        String presignedUrl =
                minioStorageService.generatePresignedUrl(quittance.getFilePath());

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

