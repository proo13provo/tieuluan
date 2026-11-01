package com.example.short_video.dto.response;

import com.example.short_video.Enum.ReportReason;
import com.example.short_video.Enum.ReportStatus;
import com.example.short_video.Enum.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private String reporterAvatarUrl;

    private ReportTargetType targetType;
    private Long targetId;
    private String targetTitle;
    private Long targetOwnerId;
    private String targetOwnerUsername;

    private ReportReason reason;
    private String description;
    private ReportStatus status;

    private Long resolvedById;
    private String resolvedByUsername;
    private String resolutionNote;
    private String actionTaken;

    private Instant createdAt;
    private Instant resolvedAt;
}
