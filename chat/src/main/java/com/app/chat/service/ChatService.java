package com.app.chat.service;

import com.app.chat.dto.ConversationDto;
import com.app.chat.model.Message;
import com.app.chat.repository.ChatRepository;
import com.app.chat.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ConversationRepository conversationRepository;

    private static final Long AGENT_ID = 1L;

    public ChatService(ChatRepository chatRepository,
                       ConversationRepository conversationRepository) {
        this.chatRepository = chatRepository;
        this.conversationRepository = conversationRepository;
    }



    public Conversation getOrCreateConversation(Long clientId) {

        Optional<Conversation> existing = conversationRepository.findByClientId(clientId);

        if (existing.isPresent()) {
            return existing.get();
        }

        Conversation conversation = new Conversation();
        conversation.setClientId(clientId);
        conversation.setAgentId(AGENT_ID);
        conversation.setCreatedAt(LocalDateTime.now());

        return conversationRepository.save(conversation);
    }

    public Conversation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));
    }

    public List<ConversationDto> getAgentConversations(Long agentId) {

        List<Conversation> conversations = conversationRepository.findByAgentId(agentId);

        return conversations.stream().map(conv -> {

            Message lastMessage =
                    chatRepository.findTopByConversationOrderByTimestampDesc(conv);

            ConversationDto dto = new ConversationDto();
            dto.setId(conv.getId());
            dto.setOtherUserId(conv.getClientId());

            String fullName = conversationRepository.findUserNameById(conv.getClientId());
            dto.setOtherUserName(fullName);

            if (lastMessage != null) {
                dto.setLastMessage(lastMessage.getContent());
                dto.setLastMessageTime(lastMessage.getTimestamp());
                dto.setLastSenderType(lastMessage.getSenderType());
            }

            dto.setUnreadCount(
                    chatRepository.countByConversationAndIsReadFalseAndReceiverId(conv, agentId)
            );

            return dto;

        }).collect(Collectors.toList());
    }

    public ConversationDto startConversation(Long clientId, Long agentIdIgnored) {

        Conversation conversation = getOrCreateConversation(clientId);

        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setOtherUserId(AGENT_ID);
        dto.setOtherUserName("Agent");
        dto.setUnreadCount(0);

        return dto;
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

    public List<Message> getMessages(Long conversationId, Long userId) {

        Conversation conversation = getConversation(conversationId);


        if (!conversation.getClientId().equals(userId)
                && !conversation.getAgentId().equals(userId)) {
            throw new RuntimeException("Accès refusé");
        }

        List<Message> messages =
                chatRepository.findByConversationOrderByTimestampAsc(conversation);

        messages.stream()
                .filter(m -> m.getReceiverId().equals(userId) && !m.isRead())
                .forEach(m -> m.setRead(true));

        chatRepository.saveAll(messages);

        return messages;
    }



    public List<Map<String, Object>> getAllClients() {

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