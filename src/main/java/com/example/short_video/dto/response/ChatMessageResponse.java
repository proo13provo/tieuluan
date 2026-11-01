package com.example.short_video.dto.response;

import com.example.short_video.Enum.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private String conversationId;

    private Long senderId;
    private String senderUsername;
    private String senderAvatarUrl;
    private Long receiverId;
    private String receiverUsername;
    private String receiverAvatarUrl;
    private String content;
    private MessageType messageType;
    private Long videoId;
    private String videoTitle;
    private String videoThumbnailUrl;
    private boolean isRead;
    private Instant createdAt;
    private boolean isMine;
}
