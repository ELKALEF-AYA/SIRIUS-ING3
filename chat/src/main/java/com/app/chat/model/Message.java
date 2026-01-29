package com.app.chat.model;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderId;
    private Long receiverId;

    private String senderType;

    private String content;

    private Long conversationId;

    private LocalDateTime timestamp = LocalDateTime.now();
}
