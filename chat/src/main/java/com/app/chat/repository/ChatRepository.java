package com.app.chat.repository;

import com.app.chat.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Message, Long> {

    List<Message> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    List<Message> findByReceiverId(Long receiverId);
}
