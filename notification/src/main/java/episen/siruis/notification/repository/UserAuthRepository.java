package episen.siruis.notification.repository;

import episen.siruis.notification.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    @Query("select u.id from UserAuth u where u.tenantId = :tenantId")
    Optional<Long> findUserIdByTenantId(@Param("tenantId") Long tenantId);
}