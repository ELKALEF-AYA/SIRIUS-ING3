package com.jsahome.kafka.consumer;

import com.jsahome.kafka.model.AccessEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessEventConsumer.class);

    @KafkaListener(
            topics = "access-events",
            groupId = "access-validation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccessEvent(AccessEvent event) {
        LOGGER.info("Événement d'accès reçu : {} {} - Accordé: {} - Raison: {}",
                event.getFirstName(),
                event.getLastName(),
                event.getAccessGranted(),
                event.getReason());

        // Ici tu peux ajouter la logique pour stocker, alerter, etc.
    }
}
