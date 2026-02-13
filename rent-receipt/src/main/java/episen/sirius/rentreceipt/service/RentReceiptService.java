package episen.sirius.rentreceipt.service;
import episen.sirius.messaging.kafka.model.RentReceiptCreatedEvent;
import episen.sirius.messaging.kafka.producer.RentReceiptEventProducer;


import episen.sirius.rentreceipt.model.Locataire;
import episen.sirius.rentreceipt.model.Logement;
import episen.sirius.rentreceipt.model.RentReceipt;
import episen.sirius.rentreceipt.repository.LocataireRepository;
import episen.sirius.rentreceipt.repository.RentReceiptRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RentReceiptService {

    private final RentReceiptRepository rentReceiptRepository;
    private final PdfService pdfService;
    private final MinioStorageService minioStorageService;
    private final LocataireRepository locataireRepository;
    private final RentReceiptEventProducer rentReceiptEventProducer;

    public RentReceiptService(
            RentReceiptRepository rentReceiptRepository,
            PdfService pdfService,
            MinioStorageService minioStorageService,
            LocataireRepository locataireRepository,
            RentReceiptEventProducer rentReceiptEventProducer
    ) {
        this.rentReceiptRepository = rentReceiptRepository;
        this.pdfService = pdfService;
        this.minioStorageService = minioStorageService;
        this.locataireRepository = locataireRepository;
        this.rentReceiptEventProducer = rentReceiptEventProducer;
    }

    public RentReceipt creerQuittance(Long locataireId, String periode) {

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
        RentReceipt rentReceipt = new RentReceipt();
        rentReceipt.setLocataire(locataire);
        rentReceipt.setLogement(logement);
        rentReceipt.setMontant(montant);
        rentReceipt.setPeriode(periode);
        rentReceipt.setCreatedAt(LocalDateTime.now());

        // Génération du PDF
        byte[] pdfBytes = pdfService.genererPdf(rentReceipt);

        // Upload MinIO
        String fileName = "quittance_" + locataire.getId() + "_" + periode.replace("/", "_") + ".pdf";
        String filePath = minioStorageService.uploadPdf(pdfBytes, fileName);

        // Sauvegarde en base
        rentReceipt.setFilePath(filePath);
        RentReceipt savedRentReceipt = rentReceiptRepository.save(rentReceipt);

        // Émission de l’événement Kafka
        RentReceiptCreatedEvent event = new RentReceiptCreatedEvent(
                savedRentReceipt.getId(),
                locataire.getId(),
                savedRentReceipt.getPeriode(),
                savedRentReceipt.getMontant()
        );

        rentReceiptEventProducer.sendQuittanceCreatedEvent(event);

        return savedRentReceipt;
    }

    public RentReceipt findById(Long id) {
        return rentReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quittance introuvable"));
    }
}
