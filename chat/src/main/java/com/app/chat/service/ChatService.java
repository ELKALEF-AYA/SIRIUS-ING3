package com.app.chat.service;

import com.app.chat.model.Message;
import com.app.chat.repository.ChatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }


    public Message saveMessage(Message message) {
        return chatRepository.save(message);
    }


    public List<Message> getConversation(Long conversationId) {
        return chatRepository.findByConversationId(conversationId);
    }


    public List<Message> getMessagesBetween(Long senderId, Long receiverId) {
        return chatRepository.findBySenderIdAndReceiverId(senderId, receiverId);
    }
}
