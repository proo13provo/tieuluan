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
public class FollowEventMessage {

    private String type; // "FOLLOW" hoặc "UNFOLLOW"
    private Long actorId;
    private String actorUsername;
    private String actorAvatarUrl;
    private boolean isMutual;
    private long followerCount;
    private Instant timestamp;
}
