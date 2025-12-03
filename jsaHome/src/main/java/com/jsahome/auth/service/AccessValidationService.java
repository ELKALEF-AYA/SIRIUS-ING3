package com.jsahome.auth.service;

import com.jsahome.auth.model.User;
import com.jsahome.auth.model.AccessLog;
import com.jsahome.auth.repository.UserRepository;
import com.jsahome.auth.repository.AccessLogRepository;
import com.jsahome.cache.model.CacheEntry;
import com.jsahome.cache.service.CacheService;
import com.jsahome.kafka.producer.AccessEventProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessValidationService.class);

    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;
    private final CacheService cacheService;
    private final AccessEventProducer accessEventProducer;

    public boolean validateAccess(String firstName, String lastName) {
        // Vérifie d'abord le cache
        CacheEntry cached = cacheService.getFromCache(firstName, lastName);
        if (cached != null) {
            LOGGER.info("Accès trouvé en cache pour {} {}", firstName, lastName);
            return cached.getAccessGranted();
        }

        // Si pas en cache, cherche en BD
        Optional<User> user = userRepository.findByFirstNameAndLastName(firstName, lastName);
        boolean accessGranted = user.isPresent();

        // Crée le log
        AccessLog log = AccessLog.builder()
                .firstName(firstName)
                .lastName(lastName)
                .accessGranted(accessGranted)
                .reason(accessGranted ? "User found" : "User not found")
                .accessTimestamp(LocalDateTime.now())
                .build();

        accessLogRepository.save(log);

        // Met en cache
        cacheService.cacheAccess(firstName, lastName, accessGranted, log.getReason());

        // Publie l'événement Kafka
        accessEventProducer.publishAccessEvent(firstName, lastName, accessGranted, log.getReason());

        return accessGranted;
    }

    public List<AccessLog> getAllLogs() {
        return accessLogRepository.findAll();
    }

    public List<AccessLog> getGrantedLogs() {
        return accessLogRepository.findByAccessGrantedTrue();
    }

    public List<AccessLog> getDeniedLogs() {
        return accessLogRepository.findByAccessGrantedFalse();
    }
}