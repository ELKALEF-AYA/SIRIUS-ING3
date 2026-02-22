package com.app.chat.controller;

import com.app.chat.dto.ClientDto;
import com.app.chat.dto.ConversationDto;
import com.app.chat.model.Conversation;
import com.app.chat.model.Message;
import com.app.chat.service.ChatService;
import com.app.chat.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Long AGENT_ID = 1L;

    public ChatController(ChatService chatService,
                          JwtService jwtService,
                          SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.jwtService = jwtService;
        this.messagingTemplate = messagingTemplate;
    }



    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        String role = extractRole(authHeader);

        log.info("[HTTP] GET /api/chat/conversations — userId={}, role={}", userId, role);

        if (userId == null || !"AGENT".equalsIgnoreCase(role)) {
            log.warn("[HTTP] GET /api/chat/conversations — Accès refusé : userId={}, role={}", userId, role);
            return ResponseEntity.status(401).build();
        }

        List<ConversationDto> conversations = chatService.getAgentConversations(userId);
        log.info("[HTTP] GET /api/chat/conversations — {} conversation(s) retournée(s) pour l'agent id={}",
                conversations.size(), userId);
        return ResponseEntity.ok(conversations);
    }



    @GetMapping("/my-conversations")
    public ResponseEntity<List<ConversationDto>> getMyConversations(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        log.info("[HTTP] GET /api/chat/my-conversations — userId={}", userId);

        if (userId == null) {
            log.warn("[HTTP] GET /api/chat/my-conversations — Token invalide ou absent");
            return ResponseEntity.status(401).build();
        }

        List<ConversationDto> conversations = chatService.getClientConversation(userId);
        log.info("[HTTP] GET /api/chat/my-conversations — {} conversation(s) retournée(s) pour client id={}",
                conversations.size(), userId);
        return ResponseEntity.ok(conversations);
    }


    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable Long conversationId,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        String role = extractRole(authHeader);
        log.info("[HTTP] GET /api/chat/conversations/{}/messages — userId={}, role={}",
                conversationId, userId, role);

        if (userId == null) {
            log.warn("[HTTP] Lecture des messages refusée — token invalide pour conversationId={}", conversationId);
            return ResponseEntity.status(401).build();
        }

        List<Message> messages = chatService.getMessages(conversationId, userId, role);
        log.info("[HTTP] GET /messages — {} message(s) retourné(s) pour conversationId={}", messages.size(), conversationId);
        return ResponseEntity.ok(messages);
    }



    @PostMapping("/conversations/start")
    public ResponseEntity<ConversationDto> startConversation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> body) {

        Long userId = extractUserId(authHeader);
        String role = extractRole(authHeader);
        Long clientId = body.get("clientId");

        log.info("[HTTP] POST /api/chat/conversations/start — userId={}, role={}, clientId={}",
                userId, role, clientId);

        if (userId == null) {
            log.warn("[HTTP] Démarrage conversation refusé — token invalide");
            return ResponseEntity.status(401).build();
        }
        if (clientId == null) {
            log.warn("[HTTP] Démarrage conversation refusé — clientId manquant dans le body");
            return ResponseEntity.badRequest().build();
        }

        boolean isAgent = "AGENT".equalsIgnoreCase(role);

        if (!isAgent && !userId.equals(clientId)) {
            log.warn("[HTTP] Démarrage conversation refusé — userId={} tente d'ouvrir pour clientId={} sans être agent",
                    userId, clientId);
            return ResponseEntity.status(403).build();
        }

        Long agentId = isAgent ? userId : AGENT_ID;
        ConversationDto dto = chatService.startConversation(clientId, agentId, role);

        log.debug("[WS] Notification NEW_CONVERSATION envoyée à client id={} et agent id={}", clientId, agentId);
        messagingTemplate.convertAndSend(
                "/topic/user/" + clientId,
                Map.of("type", "NEW_CONVERSATION", "conversationId", dto.getId())
        );
        messagingTemplate.convertAndSend(
                "/topic/user/" + agentId,
                Map.of("type", "NEW_CONVERSATION", "conversationId", dto.getId())
        );

        log.info("[HTTP] POST /conversations/start — Conversation id={} démarrée avec succès (client={}, agent={})",
                dto.getId(), clientId, agentId);
        return ResponseEntity.ok(dto);
    }



    @GetMapping("/clients")
    public ResponseEntity<List<ClientDto>> getAllClients(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        String role = extractRole(authHeader);
        log.info("[HTTP] GET /api/chat/clients — userId={}, role={}", userId, role);

        if (userId == null || !"AGENT".equalsIgnoreCase(role)) {
            log.warn("[HTTP] GET /api/chat/clients — Accès refusé : seul un agent peut lister les clients");
            return ResponseEntity.status(401).build();
        }

        List<ClientDto> clients = chatService.getAllClients();
        log.info("[HTTP] GET /api/chat/clients — {} client(s) retourné(s)", clients.size());
        return ResponseEntity.ok(clients);
    }



    @MessageMapping("/chat.send")
    public void sendMessage(Map<String, Object> payload) {

        Long conversationId = Long.valueOf(payload.get("conversationId").toString());
        Long senderId       = Long.valueOf(payload.get("senderId").toString());
        String senderType   = payload.get("senderType").toString();
        String content      = payload.get("content").toString();

        Conversation conv = chatService.getConversation(conversationId);

        Long receiverId = senderType.equalsIgnoreCase("CLIENT")
                ? conv.getAgentId()
                : conv.getClientId();

        Message message = chatService.sendMessage(
                conversationId, senderId, receiverId, senderType, content);

        messagingTemplate.convertAndSend("/topic/user/" + receiverId, message);
        messagingTemplate.convertAndSend("/topic/user/" + senderId, message);
    }



    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[AUTH] Header Authorization absent ou mal formé");
            return null;
        }
        return jwtService.extractUserId(authHeader.substring(7));
    }

    private String extractRole(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return jwtService.extractRole(authHeader.substring(7));
    }
}