package com.example.short_video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatusResponse {

    private Long targetUserId;
    private boolean isFollowing;
    private boolean isFollowedBy;
    private boolean isMutual;
    private long followerCount;
    private long followingCount;
}
