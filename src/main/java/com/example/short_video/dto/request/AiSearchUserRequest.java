package com.example.short_video.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSearchUserRequest {
    private String query;
    private int limit;
    private int page;
    private Double minScore;
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
