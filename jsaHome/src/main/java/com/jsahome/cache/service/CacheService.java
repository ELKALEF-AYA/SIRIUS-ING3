package com.jsahome.cache.service;


import com.jsahome.cache.model.CacheEntry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheService.class);
    private static final String CACHE_KEY_PREFIX = "access:";
    private static final long CACHE_DURATION_MINUTES = 5;

    private final RedisTemplate<String, CacheEntry> redisTemplate;

    /**
     * Stocke une entrée de validation en cache
     */
    public void cacheAccess(String firstName, String lastName, boolean accessGranted, String reason) {
        String cacheKey = CACHE_KEY_PREFIX + firstName + ":" + lastName;

        CacheEntry entry = CacheEntry.builder()
                .firstName(firstName)
                .lastName(lastName)
                .accessGranted(accessGranted)
                .reason(reason)
                .cachedAt(LocalDateTime.now())
                .build();

        redisTemplate.opsForValue().set(cacheKey, entry, CACHE_DURATION_MINUTES, TimeUnit.MINUTES);
        LOGGER.debug("Accès cachéisé pour {} {} : {}", firstName, lastName, accessGranted);
    }

    /**
     * Récupère une entrée du cache
     */
    public CacheEntry getFromCache(String firstName, String lastName) {
        String cacheKey = CACHE_KEY_PREFIX + firstName + ":" + lastName;
        CacheEntry entry = redisTemplate.opsForValue().get(cacheKey);

        if (entry != null) {
            LOGGER.debug("Accès trouvé en cache pour {} {}", firstName, lastName);
        }

        return entry;
    }

    /**
     * Vérifie si une entrée existe en cache
     */
    public boolean isCached(String firstName, String lastName) {
        String cacheKey = CACHE_KEY_PREFIX + firstName + ":" + lastName;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        return exists != null && exists;
    }

    /**
     * Supprime une entrée du cache
     */
    public void invalidateCache(String firstName, String lastName) {
        String cacheKey = CACHE_KEY_PREFIX + firstName + ":" + lastName;
        redisTemplate.delete(cacheKey);
        LOGGER.debug("Cache invalidé pour {} {}", firstName, lastName);
    }

    /**
     * Vide tout le cache
     */
    public void clearAllCache() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        LOGGER.info("Tout le cache a été vidé");
    }
}
