package com.jsahome.kafka.producer;

import com.jsahome.kafka.model.AccessEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessEventProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessEventProducer.class);
    private static final String TOPIC = "access-events";

    private final KafkaTemplate<String, AccessEvent> kafkaTemplate;

    public void publishAccessEvent(String firstName, String lastName, boolean accessGranted, String reason) {
        AccessEvent event = AccessEvent.builder()
                .firstName(firstName)
                .lastName(lastName)
                .accessGranted(accessGranted)
                .reason(reason)
                .eventTimestamp(LocalDateTime.now())
                .eventId(UUID.randomUUID().toString())
                .build();

        try {
            kafkaTemplate.send(TOPIC, event.getEventId(), event);
            LOGGER.info("Événement d'accès publié : {} {} - {}", firstName, lastName, accessGranted);
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la publication de l'événement", e);
        }
    }
}