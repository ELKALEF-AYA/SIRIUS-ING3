package com.jsahome.cache.service;

import com.jsahome.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheLoaderService {

    private final UserRepository userRepository;
    private final CacheService cacheService;

    /**
     * Synchronisation PostgreSQL → Redis chaque minute
     */
    @Scheduled(fixedRate = 60000)
    public void syncUsersToRedis() {

        log.info("Synchronisation des utilisateurs vers Redis…");

        userRepository.findAll().forEach(user ->
                cacheService.cacheUser(
                        user.getFirstName(),
                        user.getLastName(),
                        true,
                        "Utilisateur trouvé en base"
                )
        );

        log.info("Synchronisation Redis terminée.");
    }
}
