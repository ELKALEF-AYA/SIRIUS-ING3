package com.app.chat.controller;

import com.app.chat.dto.ConversationDto;
import com.app.chat.model.Conversation;
import com.app.chat.model.Message;
import com.app.chat.repository.ConversationRepository;
import com.app.chat.repository.ChatRepository;
import com.app.chat.service.ChatService;
import com.app.chat.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final ChatRepository chatRepository;

    private static final Long AGENT_ID = 1L;

    public ChatController(ChatService chatService,
                          JwtService jwtService,
                          SimpMessagingTemplate messagingTemplate,
                          ConversationRepository conversationRepository,
                          ChatRepository chatRepository) {
        this.chatService = chatService;
        this.jwtService = jwtService;
        this.messagingTemplate = messagingTemplate;
        this.conversationRepository = conversationRepository;
        this.chatRepository = chatRepository;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        if (userId == null || !userId.equals(AGENT_ID))
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(chatService.getAgentConversations(AGENT_ID));
    }


    @GetMapping("/my-conversations")
    public ResponseEntity<List<ConversationDto>> getMyConversations(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        if (userId == null) return ResponseEntity.status(401).build();

        Optional<Conversation> conv = conversationRepository.findByClientId(userId);
        if (conv.isEmpty()) return ResponseEntity.ok(List.of());

        ConversationDto dto = new ConversationDto();
        dto.setId(conv.get().getId());
        dto.setOtherUserId(AGENT_ID);
        dto.setOtherUserName("Agent");

        Message lastMessage = chatRepository
                .findTopByConversationOrderByTimestampDesc(conv.get());

        if (lastMessage != null) {
            dto.setLastMessage(lastMessage.getContent());
            dto.setLastMessageTime(lastMessage.getTimestamp());
            dto.setLastSenderType(lastMessage.getSenderType());
        }

        dto.setUnreadCount(
                chatRepository.countByConversationAndIsReadFalseAndReceiverId(
                        conv.get(), userId));

        return ResponseEntity.ok(List.of(dto));
    }


    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable Long conversationId,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(chatService.getMessages(conversationId, userId));
    }


    @PostMapping("/conversations/start")
    public ResponseEntity<ConversationDto> startConversation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> body) {

        Long userId = extractUserId(authHeader);
        if (userId == null || !userId.equals(AGENT_ID))
            return ResponseEntity.status(401).build();

        Long clientId = body.get("clientId");
        if (clientId == null) return ResponseEntity.badRequest().build();

        ConversationDto dto = chatService.startConversation(clientId, AGENT_ID);


        messagingTemplate.convertAndSend(
                "/topic/user/" + clientId,
                Map.of("type", "NEW_CONVERSATION", "conversationId", dto.getId())
        );

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/clients")
    public ResponseEntity<List<Map<String, Object>>> getAllClients(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        if (userId == null || !userId.equals(AGENT_ID))
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(chatService.getAllClients());
    }


    @MessageMapping("/chat.send")
    public void sendMessage(Map<String, Object> payload) {

        Long conversationId = Long.valueOf(payload.get("conversationId").toString());
        Long senderId       = Long.valueOf(payload.get("senderId").toString());
        String senderType   = payload.get("senderType").toString();
        String content      = payload.get("content").toString();

        Conversation conv = chatService.getConversation(conversationId);

        Long receiverId = senderType.equals("CLIENT")
                ? conv.getAgentId()
                : conv.getClientId();

        Message message = chatService.sendMessage(
                conversationId, senderId, receiverId, senderType, content);


        messagingTemplate.convertAndSend("/topic/user/" + receiverId, message);

        messagingTemplate.convertAndSend("/topic/user/" + senderId, message);
    }

    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return null;
        return jwtService.extractUserId(authHeader.substring(7));
    }
}