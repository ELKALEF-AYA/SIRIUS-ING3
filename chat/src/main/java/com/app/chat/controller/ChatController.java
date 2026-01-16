package com.app.chat.controller;

import com.app.chat.model.Message;
import com.app.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public ChatController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    /**
     * Receive the message send from  front via /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void sendMessage(Message message) {


        chatService.saveMessage(message);


        String destination;


        if ("CLIENT".equalsIgnoreCase(message.getSenderType())) {
            destination = "/topic/agent/" + message.getReceiverId();
        }

        else {
            destination = "/topic/client/" + message.getReceiverId();
        }


        messagingTemplate.convertAndSend(destination, message);
    }
}
