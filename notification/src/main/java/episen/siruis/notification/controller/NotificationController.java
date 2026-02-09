package episen.siruis.notification.controller;

import episen.siruis.notification.dto.NotificationDto;
import episen.siruis.notification.repository.NotificationRepository;
import episen.siruis.notification.repository.UserAuthRepository;
import episen.siruis.notification.service.JwtService;
import episen.siruis.notification.service.NotificationService;
import episen.siruis.notification.sse.SseManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserAuthRepository userAuthRepository;
    private final SseManager sseManager;

    public NotificationController(JwtService jwtService,
                                  NotificationService notificationService,
                                  NotificationRepository notificationRepository,
                                  UserAuthRepository userAuthRepository,
                                  SseManager sseManager) {
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.userAuthRepository = userAuthRepository;
        this.sseManager = sseManager;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearer(authorization);
        var user = jwtService.parse(token);

        // Autoriser uniquement CLIENT
        if (user.role() == null || !user.role().equalsIgnoreCase("CLIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seul un client peut s'abonner au flux de notifications."
            );
        }

        return sseManager.subscribe(user.userId());
    }

    @PostMapping("/simulate/invoice-generated")
    public NotificationDto simulate(@RequestBody SimulateInvoiceGenerated req) {
        if (req.tenantId() == null || req.invoiceId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les champs tenantId et invoiceId sont obligatoires."
            );
        }
        var saved = notificationService.createInvoiceGenerated(req.tenantId(), req.invoiceId());
        return NotificationDto.from(saved);
    }

    @GetMapping("/me")
    public List<NotificationDto> myNotifs(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long tenantId = resolveTenantIdFromJwt(authorization);

        return notificationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(NotificationDto::from)
                .toList();
    }

    // nombre de notifications non lues
    @GetMapping("/me/unread-count")
    public Map<String, Long> unreadCount(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long tenantId = resolveTenantIdFromJwt(authorization);
        long count = notificationRepository.countByTenantIdAndIsReadFalse(tenantId);
        return Map.of("unreadCount", count);
    }

    // Marquer une notification comme lue
    @PatchMapping("/{id}/read")
    public NotificationDto markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") Long notificationId
    ) {
        Long tenantId = resolveTenantIdFromJwt(authorization);

        var updated = notificationService.markAsRead(tenantId, notificationId);
        return NotificationDto.from(updated);
    }

    // Tout marquer comme lu
    @PatchMapping("/me/read-all")
    public Map<String, Integer> markAllAsRead(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long tenantId = resolveTenantIdFromJwt(authorization);

        int updated = notificationService.markAllAsRead(tenantId);
        return Map.of("updated", updated);
    }

    private Long resolveTenantIdFromJwt(String authorization) {
        String token = extractBearer(authorization);
        var jwtUser = jwtService.parse(token);

        var user = userAuthRepository.findById(jwtUser.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Utilisateur introuvable ou session expirée."
                ));

        Long tenantId = user.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucun locataire (tenantId) n'est associé à cet utilisateur."
            );
        }
        return tenantId;
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token manquant ou invalide."
            );
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    public record SimulateInvoiceGenerated(Long tenantId, Long invoiceId) {}
}