package com.app.chat.service;

import com.app.chat.dto.ClientDto;
import com.app.chat.dto.ConversationDto;
import com.app.chat.model.Conversation;
import com.app.chat.model.Message;
import com.app.chat.repository.ChatRepository;
import com.app.chat.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatRepository chatRepository;
    private final ConversationRepository conversationRepository;

    private static final Long AGENT_ID = 1L;

    public ChatService(ChatRepository chatRepository,
                       ConversationRepository conversationRepository) {
        this.chatRepository = chatRepository;
        this.conversationRepository = conversationRepository;
    }



    public Conversation getOrCreateConversation(Long clientId, Long agentId) {
        log.debug("[UC-CHAT] Recherche d'une conversation existante pour ce client");
        Optional<Conversation> existing = conversationRepository.findByClientId(clientId);

        if (existing.isPresent()) {
            String clientName = conversationRepository.findUserNameById(clientId);
            log.info("[UC-CHAT] Conversation existante trouvée avec le client '{}'", clientName);
            return existing.get();
        }

        String clientName = conversationRepository.findUserNameById(clientId);
        log.info("[UC-CHAT] Nouvelle conversation créée entre l'agent et le client '{}'", clientName);
        Conversation conversation = new Conversation();
        conversation.setClientId(clientId);
        conversation.setAgentId(agentId);
        conversation.setCreatedAt(LocalDateTime.now());
        Conversation saved = conversationRepository.save(conversation);
        log.info("[UC-CHAT] Conversation ouverte avec succès pour le client '{}'", clientName);
        return saved;
    }

    public Conversation getConversation(Long conversationId) {
        log.debug("[UC-CHAT] Chargement de la conversation id={}", conversationId);
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> {
                    log.warn("[UC-CHAT] Conversation introuvable : id={}", conversationId);
                    return new RuntimeException("Conversation introuvable");
                });
    }



    public List<ConversationDto> getAgentConversations(Long agentId) {
        log.info("[UC-CHAT] L'agent consulte sa liste de conversations");
        List<Conversation> conversations = conversationRepository.findByAgentId(agentId);

        List<ConversationDto> result = conversations.stream().map(conv -> {
            Message lastMessage = chatRepository.findTopByConversationOrderByTimestampDesc(conv);

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

            long unread = chatRepository.countByConversationAndIsReadFalseAndReceiverId(conv, agentId);
            dto.setUnreadCount((int) unread);

            log.debug("[UC-CHAT] Conversation avec '{}' — dernier msg: {}, non-lus: {}",
                    fullName,
                    lastMessage != null ? lastMessage.getTimestamp() : "aucun",
                    unread);

            return dto;
        }).collect(Collectors.toList());

        log.info("[UC-CHAT] {} conversation(s) retournée(s) à l'agent", result.size());
        return result;
    }


    public List<ConversationDto> getClientConversation(Long clientId) {
        String clientName = conversationRepository.findUserNameById(clientId);
        log.info("[UC-CHAT] '{}' consulte sa conversation avec l'agent", clientName);
        Optional<Conversation> conv = conversationRepository.findByClientId(clientId);

        if (conv.isEmpty()) {
            log.info("[UC-CHAT] Aucune conversation ouverte pour '{}'", clientName);
            return List.of();
        }

        ConversationDto dto = new ConversationDto();
        dto.setId(conv.get().getId());
        dto.setOtherUserId(conv.get().getAgentId());
        dto.setOtherUserName("Agent");

        Message lastMessage = chatRepository.findTopByConversationOrderByTimestampDesc(conv.get());
        if (lastMessage != null) {
            dto.setLastMessage(lastMessage.getContent());
            dto.setLastMessageTime(lastMessage.getTimestamp());
            dto.setLastSenderType(lastMessage.getSenderType());
        }

        long unread = chatRepository.countByConversationAndIsReadFalseAndReceiverId(conv.get(), clientId);
        dto.setUnreadCount((int) unread);

        log.info("[UC-CHAT] Conversation de '{}' chargée — {} message(s) non lu(s)", clientName, unread);
        return List.of(dto);
    }



    public ConversationDto startConversation(Long clientId, Long agentId, String callerRole) {
        Conversation conversation = getOrCreateConversation(clientId, agentId);

        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());

        if ("AGENT".equalsIgnoreCase(callerRole)) {
            dto.setOtherUserId(clientId);
            String fullName = conversationRepository.findUserNameById(clientId);
            dto.setOtherUserName(fullName);
            log.info("[UC-CHAT] L'agent ouvre une conversation avec le client '{}'", fullName);
        } else {
            dto.setOtherUserId(agentId);
            dto.setOtherUserName("Agent");
            String clientName = conversationRepository.findUserNameById(clientId);
            log.info("[UC-CHAT] '{}' démarre une conversation avec l'agent", clientName);
        }

        dto.setUnreadCount(0);
        return dto;
    }

    // ───── UC: Envoyer un message ─────

    public Message sendMessage(Long conversationId, Long senderId, Long receiverId,
                               String senderType, String content) {
        Conversation conversation = getConversation(conversationId);

        if (senderType.equals("CLIENT")) {
            receiverId = conversation.getAgentId();
        } else {
            receiverId = conversation.getClientId();
        }

        String senderName = conversationRepository.findUserNameById(senderId);
        String receiverName = conversationRepository.findUserNameById(receiverId);

        log.info("[UC-CHAT] '{}' envoie un message à '{}' : '{}'", senderName, receiverName, content);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);

        Message saved = chatRepository.save(message);
        log.info("[UC-CHAT] Message envoyé avec succès de '{}' à '{}'", senderName, receiverName);
        return saved;
    }



    @Transactional
    public List<Message> getMessages(Long conversationId, Long userId, String userRole) {
        Conversation conversation = getConversation(conversationId);

        boolean isClient      = conversation.getClientId() != null
                && conversation.getClientId().longValue() == userId.longValue();
        boolean isAgentUser   = "AGENT".equalsIgnoreCase(userRole);
        boolean isParticipant = conversation.getAgentId() != null
                && conversation.getAgentId().longValue() == userId.longValue();

        String userName = conversationRepository.findUserNameById(userId);

        if (!isClient && !isAgentUser && !isParticipant) {
            log.warn("[UC-CHAT] Accès refusé : '{}' n'est pas participant de cette conversation", userName);
            throw new RuntimeException("Accès refusé");
        }

        log.info("[UC-CHAT] '{}' ouvre la conversation", userName);

        List<Message> messages = chatRepository.findMessagesByConversation(conversation);

        long markedRead = messages.stream()
                .filter(m -> !m.getSenderId().equals(userId) && !m.isRead())
                .peek(m -> m.setRead(true))
                .count();

        chatRepository.saveAll(messages);

        log.info("[UC-CHAT] {} message(s) chargé(s) pour '{}' — {} marqué(s) comme lus",
                messages.size(), userName, markedRead);
        return messages;
    }



    public List<ClientDto> getAllClients() {
        log.info("[UC-CHAT] Récupération de la liste complète des clients");
        List<ClientDto> clients = conversationRepository.findAllClients()
                .stream()
                .map(obj -> new ClientDto(
                        ((Number) obj[0]).longValue(),
                        (String) obj[1]
                ))
                .toList();
        log.info("[UC-CHAT] {} client(s) trouvé(s)", clients.size());
        return clients;
    }
}