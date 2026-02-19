package com.app.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class ConversationDto {

    private Long id;
    private Long otherUserId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String lastSenderType;
    private int unreadCount;


}
