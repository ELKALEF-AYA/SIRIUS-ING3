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
      SELECT id, email, role
      FROM users
      WHERE email = ?
        AND enabled = true
        AND password_hash = crypt(?, password_hash)
    """;

        List<UserRow> rows = jdbc.query(sql, (rs, i) ->
                        new UserRow(rs.getLong("id"), rs.getString("email"), rs.getString("role")),
                email, rawPassword
        );

        return rows.stream().findFirst();
    }

    public record UserRow(Long id, String email, String role) {}
}