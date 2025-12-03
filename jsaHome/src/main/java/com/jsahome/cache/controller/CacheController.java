package com.jsahome.cache.controller;

import com.jsahome.cache.model.CacheEntry;
import com.jsahome.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CacheController {

    private final CacheService cacheService;

    @PostMapping("/store")
    public ResponseEntity<Map<String, String>> storeInCache(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam boolean accessGranted,
            @RequestParam String reason) {

        cacheService.cacheAccess(firstName, lastName, accessGranted, reason);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Accès mis en cache");
        response.put("firstName", firstName);
        response.put("lastName", lastName);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{firstName}/{lastName}")
    public ResponseEntity<CacheEntry> checkCache(
            @PathVariable String firstName,
            @PathVariable String lastName) {

        CacheEntry entry = cacheService.getFromCache(firstName, lastName);

        if (entry != null) {
            return ResponseEntity.ok(entry);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/exists/{firstName}/{lastName}")
    public ResponseEntity<Map<String, Boolean>> isCached(
            @PathVariable String firstName,
            @PathVariable String lastName) {

        boolean cached = cacheService.isCached(firstName, lastName);

        Map<String, Boolean> response = new HashMap<>();
        response.put("cached", cached);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/invalidate/{firstName}/{lastName}")
    public ResponseEntity<Map<String, String>> invalidateCache(
            @PathVariable String firstName,
            @PathVariable String lastName) {

        cacheService.invalidateCache(firstName, lastName);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache invalidé pour " + firstName + " " + lastName);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<Map<String, String>> clearAllCache() {
        cacheService.clearAllCache();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Tout le cache a été vidé");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Cache Service");

        return ResponseEntity.ok(response);
    }
}