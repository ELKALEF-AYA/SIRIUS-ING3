package episen.sirius.messaging.kafka.model;

import java.math.BigDecimal;

public class RentReceiptCreatedEvent {

    private Long quittanceId;
    private Long locataireId;
    private String periode;
    private BigDecimal montant;

    public RentReceiptCreatedEvent() {
    }

    public RentReceiptCreatedEvent(Long quittanceId, Long locataireId, String periode, BigDecimal montant) {
        this.quittanceId = quittanceId;
        this.locataireId = locataireId;
        this.periode = periode;
        this.montant = montant;
    }

    public Long getQuittanceId() {
        return quittanceId;
    }

    public void setQuittanceId(Long quittanceId) {
        this.quittanceId = quittanceId;
    }

    public Long getLocataireId() {
        return locataireId;
    }

    public void setLocataireId(Long locataireId) {
        this.locataireId = locataireId;
    }

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }
}
