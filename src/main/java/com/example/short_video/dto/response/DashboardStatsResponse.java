package com.example.short_video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private UserStats userStats;
    private VideoStats videoStats;
    private ReportStats reportStats;
    private ContentStats contentStats;

    private List<VideoResponse> topViewedVideos;
    private List<AdminUserResponse> recentUsers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private long totalUsers;
        private long activeUsers;
        private long lockedUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoStats {
        private long totalVideos;
        private long publishedVideos;
        private long hiddenVideos;
        private long deletedVideos;
        private long draftVideos;
        private long totalViews;
        private long totalLikes;
        private long totalShares;
        private long totalComments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportStats {
        private long totalReports;
        private long pendingReports;
        private long approvedReports;
        private long rejectedReports;
        private long resolvedReports;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentStats {
        private long totalCategories;
        private long totalHashtags;
    }
}
