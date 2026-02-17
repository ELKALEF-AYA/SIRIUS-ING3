package com.app.chat.service;

import com.app.chat.dto.ConversationDto;
import com.app.chat.model.Message;
import com.app.chat.repository.ChatRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public Message saveMessage(Message message) {
        if (message.getConversationId() == null) {
            throw new IllegalArgumentException("conversationId ne peut pas être null");
        }


        if (message.getSenderType() == null ||
                (!message.getSenderType().equals("AGENT") && !message.getSenderType().equals("CLIENT"))) {
            throw new IllegalArgumentException("senderType doit être AGENT ou CLIENT");
        }


        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        return chatRepository.save(message);
    }


    public List<Message> getConversation(Long conversationId) {
        if (conversationId == null || conversationId <= 0) {
            throw new IllegalArgumentException("conversationId invalide");
        }
        return chatRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }


    public List<Message> getConversationDesc(Long conversationId) {
        if (conversationId == null || conversationId <= 0) {
            throw new IllegalArgumentException("conversationId invalide");
        }
        return chatRepository.findByConversationIdOrderByTimestampDesc(conversationId);
    }


    public List<ConversationDto> getUserConversations(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId invalide");
        }


        List<Long> conversationIds = chatRepository.findDistinctConversationIdsByUserId(userId);

        if (conversationIds.isEmpty()) {
            return new ArrayList<>();
        }


        return conversationIds.stream()
                .map(convId -> buildConversationDto(convId, userId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private ConversationDto buildConversationDto(Long conversationId, Long userId) {
        try {

            Message lastMessage = chatRepository.findLastMessageByConversationId(conversationId);

            if (lastMessage == null) {
                return null;
            }


            Long otherUserId = chatRepository.findOtherUserInConversation(conversationId, userId);

            if (otherUserId == null) {
                return null;
            }

            ConversationDto dto = new ConversationDto();
            dto.setId(conversationId);
            dto.setOtherUserId(otherUserId);
            dto.setLastMessage(lastMessage.getContent());
            dto.setLastMessageTime(lastMessage.getTimestamp());
            dto.setLastSenderType(lastMessage.getSenderType());
            dto.setUnreadCount(0);

            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Message> getConversationMessages(Long conversationId, Long userId) {
        if (conversationId == null || conversationId <= 0) {
            throw new IllegalArgumentException("conversationId invalide");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId invalide");
        }

        boolean hasAccess = chatRepository.userBelongsToConversation(conversationId, userId);
        if (!hasAccess) {
            throw new SecurityException("Accès refusé à cette conversation");
        }

        return chatRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    public boolean userHasAccessToConversation(Long conversationId, Long userId) {
        return chatRepository.userBelongsToConversation(conversationId, userId);
    }

    public boolean conversationExistsBetweenUsers(Long conversationId, Long userId1, Long userId2) {
        return chatRepository.conversationExistsBetweenUsers(conversationId, userId1, userId2);
    }


    public Long getOtherUserInConversation(Long conversationId, Long userId) {
        return chatRepository.findOtherUserInConversation(conversationId, userId);
    }
}