package com.app.chat.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationDto {

    private Long id;

    private Long otherUserId;
    private String otherUserName;

    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String lastSenderType;

    private int unreadCount;
}
