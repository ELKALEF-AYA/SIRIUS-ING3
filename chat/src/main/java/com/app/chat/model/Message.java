package com.app.chat.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonProperty("conversation")
    private Conversation conversation;

    public Long getConversationId() {
        return conversation != null ? conversation.getId() : null;
    }

    private Long senderId;
    private Long receiverId;
    private String senderType;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp = LocalDateTime.now();

    @JsonProperty("isRead")
    private boolean isRead = false;
}
