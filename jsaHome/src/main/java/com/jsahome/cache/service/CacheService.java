package com.jsahome.cache.service;

import com.jsahome.cache.model.CacheEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, CacheEntry> redisTemplate;

    private static final String PREFIX = "user:";
    private static final long TTL_MINUTES = 10;

    public void cacheUser(String firstName, String lastName, boolean access, String reason) {
        String key = PREFIX + firstName + ":" + lastName;

        CacheEntry entry = CacheEntry.builder()
                .firstName(firstName)
                .lastName(lastName)
                .accessGranted(access)
                .reason(reason)
                .cachedAt(LocalDateTime.now())
                .build();

        redisTemplate.opsForValue().set(key, entry, TTL_MINUTES, TimeUnit.MINUTES);

        log.info(" User {} {} ajouté en cache Redis", firstName, lastName);
    }

    public CacheEntry getUser(String firstName, String lastName) {
        return redisTemplate.opsForValue().get(PREFIX + firstName + ":" + lastName);
    }

    public boolean exists(String firstName, String lastName) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + firstName + ":" + lastName));
    }
    public void clearAll() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.warn(" Tout le cache Redis a été vidé !");
    }

}
