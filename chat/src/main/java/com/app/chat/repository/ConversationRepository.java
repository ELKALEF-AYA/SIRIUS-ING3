package com.app.chat.repository;

import com.app.chat.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByClientId(Long clientId);

    List<Conversation> findByAgentId(Long agentId);


    @Query(value = """
        SELECT u.id,
               l.prenom || ' ' || l.nom AS fullName
        FROM users u
        JOIN locataires l ON l.id = u.tenant_id
        WHERE u.role = 'CLIENT'
          AND u.id <> 1
        ORDER BY l.nom
    """, nativeQuery = true)
    List<Object[]> findAllClients();


    @Query(value = """
        SELECT 
            CASE 
                WHEN u.role = 'AGENT' THEN 'Agent'
                ELSE COALESCE(l.prenom || ' ' || l.nom, u.email)
            END
        FROM users u
        LEFT JOIN locataires l ON l.id = u.tenant_id
        WHERE u.id = :userId
        LIMIT 1
    """, nativeQuery = true)
    String findUserNameById(@Param("userId") Long userId);
}
