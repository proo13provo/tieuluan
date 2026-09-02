package com.example.short_video.dto.response;

import lombok.*;

/**
 * DTO trả về thông tin profile người dùng cho endpoint /me
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    private long followerCount;
    private long followingCount;
    private long totalLikes;
}
