package com.example.short_video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String conversationId;
    private UserSummaryResponse friend;
    private ChatMessageResponse lastMessage;
    private long unreadCount;
    private Instant updatedAt;
}
