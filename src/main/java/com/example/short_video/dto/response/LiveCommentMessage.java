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
public class LiveCommentMessage {

    private Long liveId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String content;
    private Instant createdAt;
}
