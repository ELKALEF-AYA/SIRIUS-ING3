package com.app.chat.service;

import com.app.chat.dto.ConversationDto;
import com.app.chat.model.Conversation;
import com.app.chat.model.Message;
import com.app.chat.repository.ChatRepository;
import com.app.chat.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public Message sendMessage(Long conversationId,
                               Long senderId,
                               Long receiverId,
                               String senderType,
                               String content) {

        Conversation conversation = getConversation(conversationId);

        if (senderType.equals("CLIENT")) {
            receiverId = conversation.getAgentId();
        } else {
            receiverId = conversation.getClientId();
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);

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

        return conversationRepository.findAllClients()
                .stream()
                .map(obj -> Map.of(
                        "id", ((Number) obj[0]).longValue(),
                        "fullName", obj[1]
                ))
                .toList();
    }
}
