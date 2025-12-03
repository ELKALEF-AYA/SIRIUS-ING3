package com.jsahome.cache.model;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String firstName;
    private String lastName;
    private Boolean accessGranted;
    private String reason;
    private LocalDateTime cachedAt;
}
