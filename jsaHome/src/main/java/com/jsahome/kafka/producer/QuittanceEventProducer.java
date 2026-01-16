package com.jsahome.kafka.producer;

import com.jsahome.kafka.model.QuittanceCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class QuittanceEventProducer {

    private final KafkaTemplate<String, QuittanceCreatedEvent> kafkaTemplate;

    public QuittanceEventProducer(KafkaTemplate<String, QuittanceCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendQuittanceCreatedEvent(QuittanceCreatedEvent event) {
        kafkaTemplate.send("quittance-created", event);
    }
}
