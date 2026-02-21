package episen.siruis.notification.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class UserAuth {

    @Id
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "tenant_id")
    private Long tenantId;
}
