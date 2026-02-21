package episen.sirius.authentification.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserAuthRepository {
    private final JdbcTemplate jdbc;

    public UserAuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRow> findByEmailAndPassword(String email, String rawPassword) {
        String sql = """
          SELECT
            u.id,
            u.email,
            u.role,
            u.tenant_id,
            CASE
              WHEN u.role = 'CLIENT' THEN l.prenom
              ELSE ''
            END AS first_name,
            CASE
              WHEN u.role = 'CLIENT' THEN l.nom
              ELSE ''
            END AS last_name
          FROM users u
          LEFT JOIN locataires l ON l.id = u.tenant_id
          WHERE u.enabled = true
            AND u.email = ?
            AND u.password = ?
          LIMIT 1
        """;

        List<UserRow> rows = jdbc.query(
                sql,
                (rs, i) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getLong("tenant_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name")
                ),
                email, rawPassword
        );

        return rows.stream().findFirst();
    }

    public record UserRow(Long id, String email, String role,Long tenantId, String firstName, String lastName) {}
}