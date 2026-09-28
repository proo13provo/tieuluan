package com.example.short_video.dto.response;

import lombok.*;

/**
 * DTO trả về sau khi like / unlike video
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeResponse {
    private boolean liked;
    private long totalLikes;
}
