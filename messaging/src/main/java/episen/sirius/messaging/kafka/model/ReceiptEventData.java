package episen.sirius.messaging.kafka.model;

import java.math.BigDecimal;

public class ReceiptEventData {

    private Long id;
    private String period;
    private BigDecimal amount;

    public ReceiptEventData() {}

    public ReceiptEventData(Long id, String period, BigDecimal amount) {
        this.id = id;
        this.period = period;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public String getPeriod() { return period; }
    public BigDecimal getAmount() { return amount; }

    public void setId(Long id) { this.id = id; }
    public void setPeriod(String period) { this.period = period; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
