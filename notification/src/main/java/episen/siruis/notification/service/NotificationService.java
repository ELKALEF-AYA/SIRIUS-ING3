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
    public Notification createInvoiceGenerated(Long tenantId, Long invoiceId) {
        if (tenantId == null || invoiceId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les champs tenantId et invoiceId sont obligatoires."
            );
        }

        var user = userAuthRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun utilisateur n'est associé au locataire (tenantId=" + tenantId + ")."
                ));

        Notification n = new Notification();
        n.setTenantId(tenantId);
        n.setUserId(user.getId());
        n.setType("INVOICE_GENERATED");
        n.setTitle("Quittance disponible");
        n.setBody("Votre quittance de loyer a été générée et est disponible dans votre espace client.");
        n.setLink("/client/quittances/" + invoiceId);
        n.setRead(false);

        Notification saved = notificationRepository.save(n);

        // Push SSE (DTO)
        sseManager.send(user.getId(), NotificationDto.from(saved));

        return saved;
    }

    // Marquer une notification comme lue
    @Transactional
    public Notification markAsRead(Long tenantId, Long notificationId) {
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId manquant.");
        }
        if (notificationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant de la notification est manquant.");
        }

        Notification notif = notificationRepository.findByIdAndTenantId(notificationId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification introuvable."
                ));

        if (!notif.isRead()) {
            notif.setRead(true);
            notif = notificationRepository.save(notif);
        }

        return notif;
    }

    // Tout marquer comme lu pour ce tenantId
    @Transactional
    public int markAllAsRead(Long tenantId) {
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId manquant.");
        }
        return notificationRepository.markAllAsRead(tenantId);
    }
}