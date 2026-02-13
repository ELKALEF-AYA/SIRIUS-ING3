package episen.sirius.rentreceipt.dto;

import java.math.BigDecimal;

public class RentReceiptPreviewDTO {

    private Long locataireId;
    private String locataireNom;
    private String logementAdresse;
    private String periode;

    private BigDecimal loyer;
    private BigDecimal charges;
    private BigDecimal total;

    private String statut;


    public Long getLocataireId() {
        return locataireId;
    }

    public void setLocataireId(Long locataireId) {
        this.locataireId = locataireId;
    }

    public String getLocataireNom() {
        return locataireNom;
    }

    public void setLocataireNom(String locataireNom) {
        this.locataireNom = locataireNom;
    }

    public String getLogementAdresse() {
        return logementAdresse;
    }

    public void setLogementAdresse(String logementAdresse) {
        this.logementAdresse = logementAdresse;
    }

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public BigDecimal getLoyer() {
        return loyer;
    }

    public void setLoyer(BigDecimal loyer) {
        this.loyer = loyer;
    }

    public BigDecimal getCharges() {
        return charges;
    }

    public void setCharges(BigDecimal charges) {
        this.charges = charges;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
