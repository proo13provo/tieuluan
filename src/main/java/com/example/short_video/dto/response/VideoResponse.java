package com.example.short_video.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * DTO trả về thông tin video trong profile người dùng
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponse {
    private Long id;
    private String publicId;
    private String thumbnailUrl;
    private String title;
    private long views;
    private Double duration;
    private Long shares;
    private List<HashtagDto> hashtags;
    private List<String> categories;
    private String status;
    private Instant createdAt;
    private long likes;
    private boolean liked;
    private Long userId;
    private String username;
    private String userAvatar;
    private String matchReason;
    private Double searchScore;
}
