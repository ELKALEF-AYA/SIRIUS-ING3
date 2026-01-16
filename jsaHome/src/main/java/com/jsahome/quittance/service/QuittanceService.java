package com.jsahome.quittance.service;

import com.jsahome.kafka.model.QuittanceCreatedEvent;
import com.jsahome.kafka.producer.QuittanceEventProducer;
import com.jsahome.quittance.model.Locataire;
import com.jsahome.quittance.model.Logement;
import com.jsahome.quittance.model.Quittance;
import com.jsahome.quittance.repository.LocataireRepository;
import com.jsahome.quittance.repository.QuittanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class QuittanceService {

    private final QuittanceRepository quittanceRepository;
    private final PdfService pdfService;
    private final MinioStorageService minioStorageService;
    private final LocataireRepository locataireRepository;
    private final QuittanceEventProducer quittanceEventProducer;

    public QuittanceService(
            QuittanceRepository quittanceRepository,
            PdfService pdfService,
            MinioStorageService minioStorageService,
            LocataireRepository locataireRepository,
            QuittanceEventProducer quittanceEventProducer
    ) {
        this.quittanceRepository = quittanceRepository;
        this.pdfService = pdfService;
        this.minioStorageService = minioStorageService;
        this.locataireRepository = locataireRepository;
        this.quittanceEventProducer = quittanceEventProducer;
    }

    public Quittance creerQuittance(Long locataireId, String periode) {

        // Récupération du locataire
        Locataire locataire = locataireRepository.findById(locataireId)
                .orElseThrow(() -> new RuntimeException("Locataire introuvable"));

        // Récupération du logement
        Logement logement = locataire.getLogement();
        if (logement == null) {
            throw new RuntimeException("Aucun logement associé au locataire");
        }

        //  Calcul du montant
        BigDecimal montant = logement.getLoyer().add(logement.getCharges());

        //  Création de la quittance
        Quittance quittance = new Quittance();
        quittance.setLocataire(locataire);
        quittance.setLogement(logement);
        quittance.setMontant(montant);
        quittance.setPeriode(periode);
        quittance.setCreatedAt(LocalDateTime.now());

        // Génération du PDF
        byte[] pdfBytes = pdfService.genererPdf(quittance);

        // Upload MinIO
        String fileName = "quittance_" + locataire.getId() + "_" + periode.replace("/", "_") + ".pdf";
        String filePath = minioStorageService.uploadPdf(pdfBytes, fileName);

        // Sauvegarde en base
        quittance.setFilePath(filePath);
        Quittance savedQuittance = quittanceRepository.save(quittance);

        // Émission de l’événement Kafka
        QuittanceCreatedEvent event = new QuittanceCreatedEvent(
                savedQuittance.getId(),
                locataire.getId(),
                savedQuittance.getPeriode(),
                savedQuittance.getMontant()
        );

        quittanceEventProducer.sendQuittanceCreatedEvent(event);

        return savedQuittance;
    }

    public Quittance findById(Long id) {
        return quittanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quittance introuvable"));
    }
}
