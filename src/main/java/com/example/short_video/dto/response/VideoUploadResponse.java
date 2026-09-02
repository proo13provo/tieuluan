package com.example.short_video.dto.response;

import java.time.Instant;
import java.util.List;

public record VideoUploadResponse(
        Long id,
        Long userId,
        String username,
        String title,
        List<HashtagDto> hashtags,
        List<String> categories,
        String status,
        String publicId,
        Double duration,
        Long views,
        Long shares,
        Instant createdAt
) {
}
