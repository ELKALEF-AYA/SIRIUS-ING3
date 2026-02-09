package episen.siruis.notification.repository;

import episen.siruis.notification.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
    Optional<UserAuth> findByTenantId(Long tenantId);
}
