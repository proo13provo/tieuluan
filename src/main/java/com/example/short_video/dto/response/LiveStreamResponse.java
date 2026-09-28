package com.example.short_video.dto.response;

import com.example.short_video.Enum.LiveStreamStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LiveStreamResponse {

    private Long id;
    private Long userId;
    private String username;
    private String avatarUrl;

    private String title;
    private LiveStreamStatus status;
    private String roomId;
    private Long zegoAppId;
    private String zegoToken;
    private Integer viewerCount;
    private Instant startedAt;
    private Instant endedAt;
}
