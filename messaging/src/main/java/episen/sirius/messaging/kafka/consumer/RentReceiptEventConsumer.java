package episen.sirius.messaging.kafka.consumer;

import episen.sirius.messaging.kafka.model.RentReceiptCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import episen.sirius.messaging.kafka.model.ReceiptEventData;
import episen.sirius.messaging.kafka.model.Tenant;
import episen.sirius.messaging.kafka.model.Property;


@Service
public class RentReceiptEventConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(RentReceiptEventConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.rent-receipt-created}",
            groupId = "rent-receipt-group"
    )
    public void consume(RentReceiptCreatedEvent event) {

        ReceiptEventData receipt = event.getReceipt();
        Tenant tenant = event.getTenant();
        Property property = event.getProperty();

        System.out.println("""
                ========= RENT RECEIPT EVENT RECEIVED =========
                Event ID     : %s
                Type         : %s
                Timestamp    : %s

                Tenant       : %s %s
                Email        : %s
                Address      : %s

                Receipt ID   : %d
                Period       : %s
                Amount       : %s
                ===============================================
                """.formatted(
                event.getEventId(),
                event.getEventType(),
                event.getTimestamp(),

                tenant.getFirstName(),
                tenant.getLastName(),
                tenant.getEmail(),
                property.getAddress(),

                receipt.getId(),
                receipt.getPeriod(),
                receipt.getAmount()
        ));
    }
}
