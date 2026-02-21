package episen.sirius.messaging.kafka.producer;

import episen.sirius.messaging.kafka.model.RentReceiptCreatedEvent;
import episen.sirius.messaging.kafka.model.Tenant;
import episen.sirius.messaging.kafka.model.Property;
import episen.sirius.messaging.kafka.model.ReceiptEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RentReceiptEventProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(RentReceiptEventProducer.class);

    private final KafkaTemplate<String, RentReceiptCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.rent-receipt-created}")
    private String topic;

    public RentReceiptEventProducer(KafkaTemplate<String, RentReceiptCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRentReceiptCreatedEvent(RentReceiptCreatedEvent event) {

        String eventId = event.getEventId();

        Tenant tenant = event.getTenant();
        Property property = event.getProperty();
        ReceiptEventData receipt = event.getReceipt();

        logger.info(
                """
                ================= KAFKA EVENT =================
                Event ID    : {}
                Event Type  : {}
                Timestamp   : {}

                Tenant ID   : {}
                Tenant Name : {} {}
                Email       : {}
                Address     : {}

                Receipt ID  : {}
                Period      : {}
                Amount      : {}
                ================================================
                """,
                eventId,
                event.getEventType(),
                event.getTimestamp(),

                tenant.getId(),
                tenant.getFirstName(),
                tenant.getLastName(),
                tenant.getEmail(),
                property.getAddress(),

                receipt.getId(),
                receipt.getPeriod(),
                receipt.getAmount()
        );

        kafkaTemplate
                .send(topic,
                        String.valueOf(tenant.getId()),
                        event
                )
                .whenComplete((result, ex) -> {

                    if (ex == null) {
                        logger.info(
                                "KAFKA_SUCCESS | eventId={} | topic={} | partition={} | offset={}",
                                eventId,
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    } else {
                        logger.error(
                                "KAFKA_ERROR | eventId={} | tenantId={} | receiptId={}",
                                eventId,
                                tenant.getId(),
                                receipt.getId(),
                                ex
                        );
                    }
                });
    }
}
