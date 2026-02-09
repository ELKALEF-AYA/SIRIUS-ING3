package episen.siruis.notification.repository;

import episen.siruis.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    long countByTenantIdAndIsReadFalse(Long tenantId);

    Optional<Notification> findByIdAndTenantId(Long id, Long tenantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.tenantId = :tenantId and n.isRead = false")
    int markAllAsRead(@Param("tenantId") Long tenantId);
}