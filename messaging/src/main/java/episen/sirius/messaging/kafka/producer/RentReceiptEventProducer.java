package episen.sirius.messaging.kafka.producer;

import episen.sirius.messaging.kafka.model.RentReceiptCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class RentReceiptEventProducer {

    private final KafkaTemplate<String, RentReceiptCreatedEvent> kafkaTemplate;

    public RentReceiptEventProducer(KafkaTemplate<String, RentReceiptCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendQuittanceCreatedEvent(RentReceiptCreatedEvent event) {
        kafkaTemplate.send("quittance-created", event);
    }
}
