package com.example.short_video.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedSearchResponse {
    private String query;
    private int totalUsers;
    private int totalVideos;
    private List<UserSummaryResponse> users;
    private List<VideoResponse> videos;
}
