package com.jsahome.kafka.producer;

import com.jsahome.auth.model.AccessLog;
import com.jsahome.kafka.model.AccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(AccessEventProducer.class);

    private static final String TOPIC = "access-events";

    private final KafkaTemplate<String, AccessEvent> kafkaTemplate;

    public void publishAccessEvent(AccessLog log) {

        AccessEvent event = AccessEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .firstName(log.getFirstName())
                .lastName(log.getLastName())
                .accessGranted(log.getAccessGranted())
                .reason(log.getReason())
                .eventTimestamp(log.getAccessTimestamp())
                .build();

        try {
            kafkaTemplate.send(TOPIC, event.getEventId(), event);
            logger.info("Événement envoyé à Kafka : {}", event);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'événement Kafka", e);
        }
    }
}
