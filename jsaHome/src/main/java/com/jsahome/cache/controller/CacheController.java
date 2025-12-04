package com.jsahome.cache.controller;

import com.jsahome.cache.model.CacheEntry;
import com.jsahome.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CacheController {

    private final CacheService cacheService;

    @PostMapping("/store")
    public String store(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(defaultValue = "true") boolean accessGranted,
            @RequestParam(defaultValue = "Ajout manuel") String reason
    ) {
        cacheService.cacheUser(firstName, lastName, accessGranted, reason);
        return "✔️ Utilisateur mis en cache";
    }

    @GetMapping("/get")
    public CacheEntry get(@RequestParam String firstName, @RequestParam String lastName) {
        return cacheService.getUser(firstName, lastName);
    }

    @DeleteMapping("/clear")
    public String clearAll() {
        cacheService.clearAll();
        return "Cache vidé";
    }
}
