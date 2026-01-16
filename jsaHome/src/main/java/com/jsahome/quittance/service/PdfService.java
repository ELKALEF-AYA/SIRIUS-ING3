package com.jsahome.quittance.service;

import com.jsahome.quittance.model.Quittance;
import com.jsahome.quittance.model.Locataire;
import com.jsahome.quittance.model.Logement;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] genererPdf(Quittance quittance) {

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // ===== TITRE =====
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("QUITTANCE DE LOYER", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // ===== INFOS GÉNÉRALES =====
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            document.add(new Paragraph("Période : " + quittance.getPeriode(), valueFont));
            document.add(new Paragraph(
                    "Date d’émission : " + quittance.getCreatedAt().format(formatter),
                    valueFont
            ));
            document.add(Chunk.NEWLINE);

            // ===== PROPRIÉTAIRE (statique pour l’instant) =====
            document.add(new Paragraph("PROPRIÉTAIRE", labelFont));
            document.add(new Paragraph("Nom : JSA Home", valueFont));
            document.add(new Paragraph("Adresse : 10 rue de l’Immobilier, Paris", valueFont));
            document.add(Chunk.NEWLINE);

            // ===== LOCATAIRE =====
            Locataire locataire = quittance.getLocataire();
            if (locataire != null) {
                document.add(new Paragraph("LOCATAIRE", labelFont));
                document.add(new Paragraph(
                        "Nom : " + locataire.getPrenom() + " " + locataire.getNom(),
                        valueFont
                ));
                document.add(new Paragraph("Adresse : " + locataire.getAdresse(), valueFont));
                document.add(Chunk.NEWLINE);
            }

            // ===== LOGEMENT =====
            Logement logement = quittance.getLogement();
            if (logement != null) {
                document.add(new Paragraph("LOGEMENT", labelFont));
                document.add(new Paragraph("Adresse : " + logement.getAdresse(), valueFont));
                document.add(Chunk.NEWLINE);
            }

            // ===== DÉTAIL DU PAIEMENT =====
            document.add(new Paragraph("DÉTAIL DU PAIEMENT", labelFont));
            document.add(new Paragraph(
                    "Loyer : " + logement.getLoyer() + " €",
                    valueFont
            ));
            document.add(new Paragraph(
                    "Charges : " + logement.getCharges() + " €",
                    valueFont
            ));
            document.add(new Paragraph(
                    "Total : " + quittance.getMontant() + " €",
                    labelFont
            ));

            document.add(Chunk.NEWLINE);

            // ===== TEXTE LÉGAL =====
            Paragraph legal = new Paragraph(
                    "Je soussigné, propriétaire du logement désigné ci-dessus, "
                            + "reconnais avoir reçu la somme indiquée au titre du paiement "
                            + "du loyer pour la période mentionnée. "
                            + "La présente quittance vaut preuve de paiement.",
                    valueFont
            );
            legal.setSpacingBefore(20);
            legal.setSpacingAfter(30);
            document.add(legal);

            // ===== SIGNATURE =====
            document.add(new Paragraph("Signature du propriétaire :", labelFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("JSA Home", valueFont));

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
}
