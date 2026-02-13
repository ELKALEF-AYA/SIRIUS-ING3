package episen.sirius.messaging.kafka.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;


public class RentReceiptCreatedEvent {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;


    private String eventId;
    private String eventType;


    private ReceiptEventData receipt;
    private Tenant tenant;
    private Property property;

    public RentReceiptCreatedEvent() {
    }

    public RentReceiptCreatedEvent(String eventId,
                                   String eventType,
                                   LocalDateTime timestamp,
                                   ReceiptEventData receipt,
                                   Tenant tenant,
                                   Property property) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.receipt = receipt;
        this.tenant = tenant;
        this.property = property;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public ReceiptEventData getReceipt() { return receipt; }
    public Tenant getTenant() { return tenant; }
    public Property getProperty() { return property; }

    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setReceipt(ReceiptEventData receipt) { this.receipt = receipt; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public void setProperty(Property property) { this.property = property; }
}
