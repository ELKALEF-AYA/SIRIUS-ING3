package episen.siruis.notification.repository;


import episen.siruis.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    long countByTenantIdAndIsReadFalse(Long tenantId);
}
