package com.jsahome.kafka.consumer;

import com.jsahome.kafka.model.QuittanceCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class QuittanceEventConsumer {

    @KafkaListener(
            topics = "quittance-created",
            groupId = "quittance-group"
    )
    public void consume(QuittanceCreatedEvent event) {
        System.out.println(
                "Kafka | Quittance créée -> id=" + event.getQuittanceId()
                        + ", locataire=" + event.getLocataireId()
                        + ", période=" + event.getPeriode()
                        + ", montant=" + event.getMontant()
        );
    }
}
