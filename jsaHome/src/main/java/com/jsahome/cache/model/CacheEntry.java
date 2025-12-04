package com.jsahome.cache.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class CacheEntry implements Serializable {
    private String firstName;
    private String lastName;
    private boolean accessGranted;
    private String reason;
    private LocalDateTime cachedAt;
}
