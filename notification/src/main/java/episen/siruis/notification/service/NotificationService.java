package episen.siruis.notification.service;

import episen.siruis.notification.dto.NotificationDto;
import episen.siruis.notification.entity.Notification;
import episen.siruis.notification.repository.NotificationRepository;
import episen.siruis.notification.repository.UserAuthRepository;
import episen.siruis.notification.sse.SseManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAuthRepository userAuthRepository;
    private final SseManager sseManager;

    public NotificationService(NotificationRepository notificationRepository,
                               UserAuthRepository userAuthRepository,
                               SseManager sseManager) {
        this.notificationRepository = notificationRepository;
        this.userAuthRepository = userAuthRepository;
        this.sseManager = sseManager;
    }

    @Transactional
    public Notification createInvoiceGenerated(Long tenantId, Long receiptId, String period) {
        if (tenantId == null || receiptId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les champs tenantId et receiptId sont obligatoires."
            );
        }
        Long userId = userAuthRepository.findUserIdByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun utilisateur associé au locataire (tenantId=" + tenantId + ")."));

        Notification n = new Notification();
        n.setTenantId(tenantId);
        n.setUserId(userId);
        n.setType("RENT_RECEIPT_GENERATED");
        n.setTitle("Quittance disponible");

        String periodFr = formatPeriodFr(period);
        n.setBody("Votre quittance de loyer pour " + periodFr + " est disponible. Cliquez pour la télécharger.");
        n.setLink("/client?receiptId=" + receiptId);

        n.setRead(false);

        Notification saved = notificationRepository.save(n);
        sseManager.send(userId, NotificationDto.from(saved));
        return saved;
    }

    private String formatPeriodFr(String period) {
        if (period == null || period.isBlank()) return "ce mois";
        try {
            int year = Integer.parseInt(period.substring(0, 4));
            int month = Integer.parseInt(period.substring(5, 7));
            String[] mois = {"janvier","février","mars","avril","mai","juin","juillet","août","septembre","octobre","novembre","décembre"};
            if (month < 1 || month > 12) return period;
            return mois[month - 1] + " " + year;
        } catch (Exception e) {
            return period;
        }
    }

    @Transactional
    public Notification markAsReadByUser(Long userId, Long notificationId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId manquant.");
        }
        if (notificationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notificationId manquant.");
        }

        Notification notif = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable."));

        if (!notif.isRead()) {
            notif.setRead(true);
            notif = notificationRepository.save(notif);
        }

        return notif;
    }

    @Transactional
    public int markAllAsReadByUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId manquant.");
        }
        return notificationRepository.markAllAsReadByUser(userId);
    }
}