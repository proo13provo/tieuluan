package com.example.short_video.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendRequest {
    private Long userId;
    private int limit;
    private int page;
    private List<CandidateVideoDto> candidateVideos;
    private List<WatchHistoryItemDto> watchHistory;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateVideoDto {
        private Long id;
        private String title;
        private long views;
        private long likes;
        private long shares;
        private Double duration;
        private String createdAt;
        private List<String> categories;
        private List<String> hashtags;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WatchHistoryItemDto {
        private Long videoId;
        private Double watchedDuration;
        private Double duration;
        private Boolean completed;
        private Integer watchCount;
        private Boolean isLiked;
        private Boolean isCommented;
        private Boolean isShared;
        private Boolean isSkipped;
        private String lastWatchedAt;
        private List<String> categories;
        private List<String> hashtags;
    }
}
