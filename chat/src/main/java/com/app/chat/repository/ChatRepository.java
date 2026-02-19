package com.app.chat.repository;

import com.app.chat.model.Conversation;
import com.app.chat.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRepository extends JpaRepository<Message, Long> {

    Message findTopByConversationOrderByTimestampDesc(Conversation conversation);

    List<Message> findByConversationOrderByTimestampAsc(Conversation conversation);

    int countByConversationAndIsReadFalseAndReceiverId(Conversation conversation, Long receiverId);


    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.timestamp ASC")
    List<Message> findMessagesByConversation(@Param("conversation") Conversation conversation);
}
