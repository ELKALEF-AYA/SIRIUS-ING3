package com.app.chat.repository;

import com.app.chat.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRepository extends JpaRepository<Message, Long> {


    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);


    List<Message> findByConversationIdOrderByTimestampDesc(Long conversationId);


    @Query(value = """
            SELECT DISTINCT conversation_id FROM message 
            WHERE sender_id = :userId OR receiver_id = :userId 
            ORDER BY conversation_id DESC
            """, nativeQuery = true)
    List<Long> findDistinctConversationIdsByUserId(@Param("userId") Long userId);


    @Query(value = """
            SELECT * FROM message 
            WHERE conversation_id = :conversationId 
            ORDER BY timestamp DESC 
            LIMIT 1
            """, nativeQuery = true)
    Message findLastMessageByConversationId(@Param("conversationId") Long conversationId);


    @Query(value = """
            SELECT COUNT(*) > 0 FROM message 
            WHERE conversation_id = :conversationId 
            AND (sender_id = :userId OR receiver_id = :userId)
            """, nativeQuery = true)
    boolean userBelongsToConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);


    @Query(value = """
            SELECT DISTINCT 
                CASE 
                    WHEN sender_id = :userId THEN receiver_id 
                    ELSE sender_id 
                END as other_user_id
            FROM message 
            WHERE conversation_id = :conversationId 
            AND (sender_id = :userId OR receiver_id = :userId)
            LIMIT 1
            """, nativeQuery = true)
    Long findOtherUserInConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);


    @Query(value = """
            SELECT COUNT(*) > 0 FROM message 
            WHERE conversation_id = :conversationId 
            AND ((sender_id = :userId1 AND receiver_id = :userId2) 
                 OR (sender_id = :userId2 AND receiver_id = :userId1))
            """, nativeQuery = true)
    boolean conversationExistsBetweenUsers(
            @Param("conversationId") Long conversationId,
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );
}
