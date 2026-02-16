package episen.sirius.rentreceipt.service;

import episen.sirius.messaging.kafka.model.RentReceiptCreatedEvent;
import episen.sirius.messaging.kafka.producer.RentReceiptEventProducer;
import episen.sirius.rentreceipt.dto.RentReceiptPreviewDTO;
import episen.sirius.rentreceipt.model.Locataire;
import episen.sirius.rentreceipt.model.Logement;
import episen.sirius.rentreceipt.model.RentReceipt;
import episen.sirius.rentreceipt.repository.LocataireRepository;
import episen.sirius.rentreceipt.repository.RentReceiptRepository;
import org.springframework.stereotype.Service;
import episen.sirius.messaging.kafka.model.ReceiptEventData;
import episen.sirius.messaging.kafka.model.Tenant;
import episen.sirius.messaging.kafka.model.Property;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


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

    public RentReceipt creerQuittance(Long locataireId, String periode, BigDecimal chargesDuMois) {

        Locataire locataire = locataireRepository.findById(locataireId)
                .orElseThrow(() -> new RuntimeException("Locataire introuvable"));

        Logement logement = locataire.getLogement();
        if (logement == null) {
            throw new RuntimeException("Aucun logement associé au locataire");
        }

        BigDecimal montant = logement.getLoyer().add(chargesDuMois);

        RentReceipt rentReceipt = new RentReceipt();
        rentReceipt.setLocataire(locataire);
        rentReceipt.setLogement(logement);
        rentReceipt.setPeriode(periode);
        rentReceipt.setCharges(chargesDuMois);
        rentReceipt.setMontant(montant);
        rentReceipt.setCreatedAt(LocalDateTime.now());

        RentReceipt savedRentReceipt = rentReceiptRepository.save(rentReceipt);

        byte[] pdfBytes = pdfService.genererPdf(savedRentReceipt);

        String fileName = "Rent_Receipt_" + locataire.getId() + "_" + periode.replace("/", "_") + ".pdf";

        String filePath = minioStorageService.uploadPdf(pdfBytes, fileName);

        savedRentReceipt.setFilePath(filePath);
        savedRentReceipt = rentReceiptRepository.save(savedRentReceipt);

        ReceiptEventData receipt = new ReceiptEventData(
                savedRentReceipt.getId(),
                savedRentReceipt.getPeriode(),
                savedRentReceipt.getMontant()
        );

        Tenant tenant = new Tenant(
                locataire.getId(),
                locataire.getPrenom(),
                locataire.getNom(),
                locataire.getEmail()
        );

        Property property = new Property(
                logement.getAdresse()
        );

        RentReceiptCreatedEvent event = new RentReceiptCreatedEvent(
                UUID.randomUUID().toString(),
                "RENT_RECEIPT_CREATED",
                LocalDateTime.now(),
                receipt,
                tenant,
                property
        );

        rentReceiptEventProducer.sendRentReceiptCreatedEvent(event);



        return savedRentReceipt;
    }

    public RentReceipt findById(Long id) {
        return rentReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quittance introuvable"));
    }

    public RentReceiptPreviewDTO previewQuittance(Long locataireId, String periode, BigDecimal chargesDuMois) {

        Locataire locataire = locataireRepository.findById(locataireId)
                .orElseThrow(() -> new RuntimeException("Locataire introuvable"));

        Logement logement = locataire.getLogement();
        if (logement == null) {
            throw new RuntimeException("Aucun logement associé au locataire");
        }

        RentReceiptPreviewDTO dto = new RentReceiptPreviewDTO();
        dto.setLocataireId(locataire.getId());
        dto.setLocataireNom(locataire.getPrenom() + " " + locataire.getNom());
        dto.setLogementAdresse(logement.getAdresse());
        dto.setPeriode(periode);

        dto.setLoyer(logement.getLoyer());
        dto.setCharges(chargesDuMois);
        dto.setTotal(logement.getLoyer().add(chargesDuMois));
        dto.setStatut("PAYÉ");

        return dto;
    }

    public List<RentReceipt> getQuittancesByLocataire(Long locataireId) {
        return rentReceiptRepository.findByLocataire_Id(locataireId);
    }
}
