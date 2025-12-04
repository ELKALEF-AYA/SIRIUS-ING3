package com.jsahome.auth.service;

import com.jsahome.auth.model.AccessLog;
import com.jsahome.auth.repository.AccessLogRepository;
import com.jsahome.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessValidationService {

    private final CacheService cacheService;
    private final AccessLogRepository accessLogRepository;

    public AccessLog validateAccess(String firstName, String lastName) {

        boolean exists = cacheService.exists(firstName, lastName);


        AccessLog log = AccessLog.builder()
                .firstName(firstName)
                .lastName(lastName)
                .accessGranted(exists)
                .reason(exists ? "User found in Redis" : "User not found")
                .build();


        return accessLogRepository.save(log);
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