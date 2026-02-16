package episen.siruis.notification.kafkaConsumer;

import episen.sirius.messaging.kafka.model.RentReceiptCreatedEvent;
import episen.siruis.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class RentReceiptEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RentReceiptEventConsumer.class);

    private final NotificationService notificationService;

    public RentReceiptEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.rent-receipt-created}",
            groupId = "${app.kafka.consumer.group-id:notification-group}"
    )
    public void consume(RentReceiptCreatedEvent event) {

        if (event == null || event.getTenant() == null || event.getReceipt() == null) {
            logger.warn("Event Kafka invalide : tenant/receipt manquant");
            return;
        }

        Long tenantId = event.getTenant().getId();
        Long receiptId = event.getReceipt().getId();
        String period = event.getReceipt().getPeriod();

        if (tenantId == null || receiptId == null) {
            logger.warn("Event Kafka invalide : tenantId ou receiptId manquant");
            return;
        }

        logger.info("rent-receipt-created reçu | tenantId={} | receiptId={}", tenantId, receiptId);

        try {
            notificationService.createInvoiceGenerated(tenantId, receiptId, period);
            logger.info("Notification créée | tenantId={} | receiptId={}", tenantId, receiptId);
        } catch (Exception e) {
            logger.error("Erreur lors de la création notification | tenantId={} | receiptId={}", tenantId, receiptId, e);
        }
    }
}