package com.jsahome.auth.service;

import com.jsahome.auth.model.User;
import com.jsahome.auth.model.AccessLog;
import com.jsahome.auth.repository.UserRepository;
import com.jsahome.auth.repository.AccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessValidationService {

    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;

    public boolean validateAccess(String firstName, String lastName) {
        Optional<User> user = userRepository.findByFirstNameAndLastName(firstName, lastName);

        AccessLog log = AccessLog.builder()
                .firstName(firstName)
                .lastName(lastName)
                .accessGranted(user.isPresent())
                .reason(user.isPresent() ? "User found" : "User not found")
                .accessTimestamp(LocalDateTime.now())
                .build();

        accessLogRepository.save(log);

        return user.isPresent();
    }

    public java.util.List<AccessLog> getAllLogs() {
        return accessLogRepository.findAll();
    }

    public java.util.List<AccessLog> getGrantedLogs() {
        return accessLogRepository.findByAccessGrantedTrue();
    }

    public java.util.List<AccessLog> getDeniedLogs() {
        return accessLogRepository.findByAccessGrantedFalse();
    }
}
