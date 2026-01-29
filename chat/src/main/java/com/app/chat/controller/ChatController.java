package com.app.chat.controller;

import com.app.chat.model.Message;
import com.app.chat.service.ChatService;
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

    /**
     * Receive a STOMP message from the front via /app/chat.send
     * WebSocket real-time messaging
     */
    @MessageMapping("/chat.send")
    public void sendMessage(Message message) {

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
    }

    /**
     * REST endpoint to retrieve conversation history
     * GET /api/chat/history/{conversationId}
     */
    @GetMapping("/history/{conversationId}")
    public List<Message> getConversationHistory(@PathVariable Long conversationId) {
        return chatService.getConversation(conversationId);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public String health() {
        return "Chat service is running on port 8081 ";
    }
}