package com.jsahome.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private Boolean accessGranted;

    @Column(length = 255)
    private String reason;

    @Column(name = "access_timestamp", nullable = false)
    private LocalDateTime accessTimestamp;

    @PrePersist
    protected void onCreate() {
        if (accessTimestamp == null) {
            accessTimestamp = LocalDateTime.now();
        }
    }
}