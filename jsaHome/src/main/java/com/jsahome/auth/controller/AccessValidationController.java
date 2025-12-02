package com.jsahome.auth.controller;

import com.jsahome.auth.model.AccessLog;
import com.jsahome.auth.service.AccessValidationService;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;
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

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateAccess(
            @RequestParam String firstName,
            @RequestParam String lastName) {

        boolean accessGranted = accessValidationService.validateAccess(firstName, lastName);

        Map<String, Object> response = new HashMap<>();
        response.put("accessGranted", accessGranted);
        response.put("message", accessGranted ? "Accès accordé" : "Accès refusé");
        response.put("firstName", firstName);
        response.put("lastName", lastName);

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