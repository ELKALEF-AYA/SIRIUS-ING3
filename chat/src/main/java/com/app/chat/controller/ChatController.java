package com.app.chat.controller;

import com.app.chat.dto.ConversationDto;
import com.app.chat.model.Message;
import com.app.chat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

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
        if (userId == null || !userId.equals(AGENT_ID))
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(chatService.getAgentConversations(AGENT_ID));
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

        return ResponseEntity.ok(chatService.startConversation(clientId, AGENT_ID));
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
        Long senderId = Long.valueOf(payload.get("senderId").toString());
        String senderType = payload.get("senderType").toString();
        String content = payload.get("content").toString();

        Conversation conv = chatService.getConversation(conversationId);

        Long receiverId = senderType.equals("CLIENT")
                ? conv.getAgentId()
                : conv.getClientId();

        Message message = chatService.sendMessage(
                conversationId, senderId, receiverId, senderType, content
        );

        messagingTemplate.convertAndSend("/topic/user/" + receiverId, message);
    }

    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return null;

        return jwtService.extractUserId(authHeader.substring(7));


    @MessageMapping("/chat.send")
    public void sendMessage(Message message) {
        try {

            chatService.saveMessage(message);
            String destination;

            if ("CLIENT".equalsIgnoreCase(message.getSenderType())) {
                destination = "/topic/agent/" + message.getReceiverId();
            }
            else if ("AGENT".equalsIgnoreCase(message.getSenderType())) {
                destination = "/topic/client/" + message.getReceiverId();
            }
            else {
                System.err.println("Invalid senderType: " + message.getSenderType());
                return;
            }

            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }


    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getUserConversations(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserIdFromJWT(authHeader);

            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().build();
            }

            List<ConversationDto> conversations = chatService.getUserConversations(userId);
            return ResponseEntity.ok(conversations);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des conversations: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }


    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<Message>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserIdFromJWT(authHeader);

            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().build();
            }


            List<Message> messages = chatService.getConversationMessages(conversationId, userId);
            return ResponseEntity.ok(messages);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des messages: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }


    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<Message>> getConversationHistory(@PathVariable Long conversationId) {
        try {
            List<Message> messages = chatService.getConversation(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de l'historique: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }


    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat service is running on port 8083");
    }


    private Long extractUserIdFromJWT(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Authorization header invalide ou absent");
            return null;
        }

        try {
            String token = authHeader.substring(7);

            System.out.println("Token reçu: " + token.substring(0, Math.min(20, token.length())) + "...");


            return null;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction du JWT: " + e.getMessage());
            return null;
        }

    }
}
