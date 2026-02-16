package com.app.chat.controller;

import com.app.chat.dto.ConversationDto;
import com.app.chat.model.Message;
import com.app.chat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public ChatController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }


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