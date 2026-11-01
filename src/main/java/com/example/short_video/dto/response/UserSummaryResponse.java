package com.example.short_video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    private boolean isFollowing;
    private boolean isFollowedBy;
    private boolean isMutual;
    private Long followerCount;
    private String matchReason;
}
