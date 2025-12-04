package com.jsahome.auth.controller;

import com.jsahome.auth.model.AccessLog;
import com.jsahome.auth.service.AccessValidationService;
import com.jsahome.kafka.producer.AccessEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/auth/access")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccessValidationController {

    private final AccessValidationService accessValidationService;
    private final AccessEventProducer accessEventProducer;

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateAccess(
            @RequestParam String firstName,
            @RequestParam String lastName) {

        AccessLog log = accessValidationService.validateAccess(firstName, lastName);


        accessEventProducer.publishAccessEvent(log);

        Map<String, Object> response = new HashMap<>();
        response.put("accessGranted", log.getAccessGranted());
        response.put("reason", log.getReason());
        response.put("timestamp", log.getAccessTimestamp());

        return ResponseEntity.ok(response);
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