package episen.sirius.messaging.kafka.consumer;

import episen.sirius.messaging.kafka.model.RentReceiptCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class RentReceiptEventConsumer {

    @KafkaListener(
            topics = "quittance-created",
            groupId = "quittance-group"
    )
    public void consume(RentReceiptCreatedEvent event) {
        System.out.println(
                "Kafka | Quittance créée -> id=" + event.getQuittanceId()
                        + ", locataire=" + event.getLocataireId()
                        + ", période=" + event.getPeriode()
                        + ", montant=" + event.getMontant()
        );
    }
}
