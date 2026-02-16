package episen.siruis.notification.controller;

import episen.siruis.notification.dto.NotificationDto;
import episen.siruis.notification.repository.NotificationRepository;
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
    private final SseManager sseManager;

    public NotificationController(JwtService jwtService,
                                  NotificationService notificationService,
                                  NotificationRepository notificationRepository,
                                  SseManager sseManager) {
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.sseManager = sseManager;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader(value = "Authorization", required = false) String authorization) {
        var jwtUser = parseJwt(authorization);

        if (jwtUser.role() == null || !jwtUser.role().equalsIgnoreCase("CLIENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul un client peut s'abonner au flux.");
        }

        Long userId = jwtUser.userId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "userId absent du token.");
        }

        // SSE par userId
        return sseManager.subscribe(userId);
    }

    @GetMapping("/me")
    public List<NotificationDto> myNotifs(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = requireUserId(authorization);

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDto::from)
                .toList();
    }

    @GetMapping("/me/unread-count")
    public Map<String, Long> unreadCount(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = requireUserId(authorization);
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return Map.of("unreadCount", count);
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") Long notificationId
    ) {
        Long userId = requireUserId(authorization);
        var updated = notificationService.markAsReadByUser(userId, notificationId);
        return NotificationDto.from(updated);
    }

    @PatchMapping("/me/read-all")
    public Map<String, Integer> markAllAsRead(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = requireUserId(authorization);
        int updated = notificationService.markAllAsReadByUser(userId);
        return Map.of("updated", updated);
    }

    private JwtService.JwtUser parseJwt(String authorization) {
        String token = extractBearer(authorization);
        return jwtService.parse(token);
    }

    private Long requireUserId(String authorization) {
        var jwtUser = parseJwt(authorization);
        if (jwtUser.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "userId absent du token.");
        }
        return jwtUser.userId();
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token manquant ou invalide.");
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}