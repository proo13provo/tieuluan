package com.example.short_video.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

/**
 * DTO trả về thông tin một comment / reply
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Long parentCommentId;
    private long replyCount;
    private Instant createdAt;
}
