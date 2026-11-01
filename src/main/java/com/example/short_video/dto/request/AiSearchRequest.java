package com.example.short_video.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSearchRequest {
    private String query;
    private int limit;
    private int page;
    private String sortBy;
    private Double minScore;
    private List<AiRecommendRequest.CandidateVideoDto> candidateVideos;
    private List<CandidateUserDto> candidateUsers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateUserDto {
        private Long id;
        private String username;
        private String bio;
        private String avatarUrl;
    }
}
