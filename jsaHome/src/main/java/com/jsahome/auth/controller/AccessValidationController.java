package com.jsahome.auth.controller;
import com.jsahome.auth.model.AccessLog;
import com.jsahome.auth.service.AccessValidationService;
import com.jsahome.kafka.producer.AccessEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/access")
@RequiredArgsConstructor
@Getter
@Setter
@CrossOrigin(origins = "*")
public class AccessValidationController {
    private final AccessValidationService accessValidationService;
    private final AccessEventProducer accessEventProducer;

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateAccess(
            @RequestParam String firstName,
            @RequestParam String lastName) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userRepository.findByFirstNameAndLastName(firstName, lastName);

            if (user != null) {
                response.put("accessGranted", true);
                response.put("reason", "User found");
            } else {
                response.put("accessGranted", false);
                response.put("reason", "User not found");
            }

            // Log to database
            AccessLog log = new AccessLog();
            log.setFirstName(firstName);
            log.setLastName(lastName);
            log.setAccessGranted((Boolean) response.get("accessGranted"));
            log.setReason((String) response.get("reason"));
            log.setAccessTimestamp(LocalDateTime.now());
            accessLogRepository.save(log);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("accessGranted", false);
            response.put("reason", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/publish-event")
    public ResponseEntity<Map<String, Object>> publishAccessEvent(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false, defaultValue = "true") boolean accessGranted,
            @RequestParam(required = false, defaultValue = "Manual access event") String reason) {
        try {
            accessEventProducer.publishAccessEvent(firstName, lastName, accessGranted, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Événement d'accès publié avec succès");
            response.put("firstName", firstName);
            response.put("lastName", lastName);
            response.put("accessGranted", accessGranted);
            response.put("reason", reason);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Erreur lors de la publication: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AccessLog>> getAllLogs() {
        return ResponseEntity.ok(accessValidationService.getAllLogs());
    }

    @GetMapping("/logs/granted")
    public ResponseEntity<List<AccessLog>> getGrantedLogs() {
        return ResponseEntity.ok(accessValidationService.getGrantedLogs());
    }

    @GetMapping("/logs/denied")
    public ResponseEntity<List<AccessLog>> getDeniedLogs() {
        return ResponseEntity.ok(accessValidationService.getDeniedLogs());
    }
}