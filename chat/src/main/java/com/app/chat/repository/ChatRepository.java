package com.app.chat.repository;

import com.app.chat.model.Conversation;
import com.app.chat.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Message, Long> {


    Message findTopByConversationOrderByTimestampDesc(Conversation conversation);


    List<Message> findByConversationOrderByTimestampAsc(Conversation conversation);


    int countByConversationAndIsReadFalseAndReceiverId(
            Conversation conversation,
            Long receiverId
    );
}
