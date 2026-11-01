package com.example.short_video.service;

import com.example.short_video.Enum.ReportStatus;
import com.example.short_video.Enum.ReportTargetType;
import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.request.ProcessReportRequest;
import com.example.short_video.dto.request.UpdateVideoStatusRequest;
import com.example.short_video.dto.request.WarnUserRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.ReportResponse;
import com.example.short_video.dto.response.UserWarningResponse;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.entity.Comment;
import com.example.short_video.entity.Report;
import com.example.short_video.entity.User;
import com.example.short_video.entity.UserWarning;
import com.example.short_video.entity.Video;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.mapper.VideoMapper;
import com.example.short_video.repository.CommentRepository;
import com.example.short_video.repository.ReportRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.repository.UserWarningRepository;
import com.example.short_video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModeratorService {

    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final UserWarningRepository userWarningRepository;
    private final VideoMapper videoMapper;

    @Transactional
    public ApiResponse<VideoResponse> updateVideoStatus(Long videoId, UpdateVideoStatusRequest request, User moderator) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với ID: " + videoId));

        VideoStatus oldStatus = video.getStatus();
        video.setStatus(request.getStatus());
        Video updated = videoRepository.save(video);

        log.info("Moderator {} đã đổi trạng thái video ID {} từ {} sang {}. Lý do: {}",
                moderator.getUsername(), videoId, oldStatus, request.getStatus(), request.getReason());

        return ApiResponse.success("Đã cập nhật trạng thái video thành công sang: " + request.getStatus(),
                videoMapper.toVideoResponse(updated));
    }

    @Transactional
    public ApiResponse<Void> deleteViolatingVideo(Long videoId, String reason, User moderator) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với ID: " + videoId));
        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);

        log.warn("Moderator {} đã xóa vi phạm video ID {}. Lý do: {}", moderator.getUsername(), videoId, reason);
        return ApiResponse.success("Đã xóa video vi phạm thành công");
    }

    // ─── 2. KHÓA / MỞ KHÓA BÌNH LUẬN TRÊN VIDEO VÀ XÓA BÌNH LUẬN VI PHẠM ────────

    @Transactional
    public ApiResponse<VideoResponse> toggleVideoComments(Long videoId, boolean locked, User moderator) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với ID: " + videoId));

        // iscomment = true: cho phép bình luận; iscomment = false: khóa bình luận
        video.setIscomment(!locked);
        Video updated = videoRepository.save(video);

        String msg = locked ? "Đã khóa tính năng bình luận cho video này" : "Đã mở khóa bình luận cho video này";
        log.info("Moderator {} đã thay đổi khóa bình luận video ID {}: locked={}", moderator.getUsername(), videoId, locked);

        return ApiResponse.success(msg, videoMapper.toVideoResponse(updated));
    }

    @Transactional
    public ApiResponse<Void> deleteViolatingComment(Long commentId, String reason, User moderator) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với ID: " + commentId));

        commentRepository.delete(comment);
        log.warn("Moderator {} đã xóa bình luận vi phạm ID {}. Lý do: {}", moderator.getUsername(), commentId, reason);

        return ApiResponse.success("Đã xóa bình luận vi phạm thành công");
    }

    @Transactional
    public ApiResponse<UserWarningResponse> warnUser(Long userId, WarnUserRequest request, User moderator) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        UserWarning warning = UserWarning.builder()
                .user(user)
                .moderator(moderator)
                .reason(request.getReason())
                .build();
        UserWarning saved = userWarningRepository.save(warning);

        user.incrementWarningCount();
        userRepository.save(user);

        log.warn("Moderator {} đã gửi cảnh cáo tới user {} (ID {}). Số lần cảnh cáo hiện tại: {}. Lý do: {}",
                moderator.getUsername(), user.getUsername(), user.getId(), user.getWarningCount(), request.getReason());

        UserWarningResponse response = UserWarningResponse.builder()
                .id(saved.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .moderatorId(moderator.getId())
                .moderatorUsername(moderator.getUsername())
                .reason(saved.getReason())
                .createdAt(saved.getCreatedAt())
                .build();

        return ApiResponse.success("Đã gửi cảnh cáo người dùng thành công (Tổng cảnh cáo: " + user.getWarningCount() + ")", response);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<UserWarningResponse>> getUserWarnings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        List<UserWarningResponse> warnings = userWarningRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(w -> UserWarningResponse.builder()
                        .id(w.getId())
                        .userId(w.getUser().getId())
                        .username(w.getUser().getUsername())
                        .moderatorId(w.getModerator() != null ? w.getModerator().getId() : null)
                        .moderatorUsername(w.getModerator() != null ? w.getModerator().getUsername() : null)
                        .reason(w.getReason())
                        .createdAt(w.getCreatedAt())
                        .build())
                .toList();

        return ApiResponse.success("Lấy danh sách cảnh cáo thành công", warnings);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<ReportResponse>> getReports(ReportStatus status, ReportTargetType targetType, int page, int size) {
        int targetPage = Math.max(0, page);
        int targetSize = (size > 0 && size <= 100) ? size : 20;
        Pageable pageable = PageRequest.of(targetPage, targetSize);

        Page<Report> reports;
        if (status != null && targetType != null) {
            reports = reportRepository.findByStatusAndTargetTypeOrderByCreatedAtDesc(status, targetType, pageable);
        } else if (status != null) {
            reports = reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (targetType != null) {
            reports = reportRepository.findByTargetTypeOrderByCreatedAtDesc(targetType, pageable);
        } else {
            reports = reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Page<ReportResponse> responses = reports.map(this::mapToReportResponse);
        return ApiResponse.success("Lấy danh sách báo cáo thành công", responses);
    }

    @Transactional(readOnly = true)
    public ApiResponse<ReportResponse> getReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo với ID: " + reportId));

        return ApiResponse.success("Lấy thông tin chi tiết báo cáo thành công", mapToReportResponse(report));
    }

    @Transactional
    public ApiResponse<ReportResponse> processReport(Long reportId, ProcessReportRequest request, User moderator) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo với ID: " + reportId));

        report.setStatus(request.getStatus());
        report.setResolvedBy(moderator);
        report.setResolutionNote(request.getResolutionNote());
        report.setActionTaken(request.getActionTaken());
        report.setResolvedAt(Instant.now());

        if (request.getActionTaken() != null && !request.getActionTaken().isBlank()) {
            executeReportAction(report, request.getActionTaken(), request.getResolutionNote(), moderator);
        }

        Report saved = reportRepository.save(report);
        log.info("Moderator {} đã xử lý báo cáo ID {} thành status: {}, action: {}",
                moderator.getUsername(), reportId, request.getStatus(), request.getActionTaken());

        return ApiResponse.success("Xử lý báo cáo thành công", mapToReportResponse(saved));
    }

    private void executeReportAction(Report report, String action, String note, User moderator) {
        String cleanAction = action.trim().toUpperCase();
        try {
            if ("HIDE_VIDEO".equals(cleanAction) && report.getTargetType() == ReportTargetType.VIDEO) {
                videoRepository.findById(report.getTargetId()).ifPresent(v -> {
                    v.setStatus(VideoStatus.HIDDEN);
                    videoRepository.save(v);
                });
            } else if ("DELETE_VIDEO".equals(cleanAction) && report.getTargetType() == ReportTargetType.VIDEO) {
                videoRepository.findById(report.getTargetId()).ifPresent(v -> {
                    v.setStatus(VideoStatus.DELETED);
                    videoRepository.save(v);
                });
            } else if ("LOCK_COMMENTS".equals(cleanAction) && report.getTargetType() == ReportTargetType.VIDEO) {
                videoRepository.findById(report.getTargetId()).ifPresent(v -> {
                    v.setIscomment(false);
                    videoRepository.save(v);
                });
            } else if ("DELETE_COMMENT".equals(cleanAction) && report.getTargetType() == ReportTargetType.COMMENT) {
                commentRepository.findById(report.getTargetId()).ifPresent(commentRepository::delete);
            } else if ("WARN_USER".equals(cleanAction)) {
                User targetUser = resolveTargetUser(report);
                if (targetUser != null) {
                    UserWarning warning = UserWarning.builder()
                            .user(targetUser)
                            .moderator(moderator)
                            .reason(note != null ? note : "Vi phạm tiêu chuẩn cộng đồng từ báo cáo ID " + report.getId())
                            .build();
                    userWarningRepository.save(warning);
                    targetUser.incrementWarningCount();
                    userRepository.save(targetUser);
                }
            } else if ("BAN_USER".equals(cleanAction)) {
                User targetUser = resolveTargetUser(report);
                if (targetUser != null) {
                    targetUser.setIsActive(false);
                    userRepository.save(targetUser);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi tự động thực thi hành động {} cho báo cáo ID {}: {}", cleanAction, report.getId(), e.getMessage());
        }
    }

    private User resolveTargetUser(Report report) {
        if (report.getTargetType() == ReportTargetType.USER) {
            return userRepository.findById(report.getTargetId()).orElse(null);
        } else if (report.getTargetType() == ReportTargetType.VIDEO) {
            return videoRepository.findById(report.getTargetId()).map(Video::getUser).orElse(null);
        } else if (report.getTargetType() == ReportTargetType.COMMENT) {
            return commentRepository.findById(report.getTargetId()).map(Comment::getUser).orElse(null);
        }
        return null;
    }

    private ReportResponse mapToReportResponse(Report report) {
        String targetTitle = null;
        Long targetOwnerId = null;
        String targetOwnerUsername = null;

        if (report.getTargetType() == ReportTargetType.VIDEO) {
            Optional<Video> videoOpt = videoRepository.findById(report.getTargetId());
            if (videoOpt.isPresent()) {
                targetTitle = videoOpt.get().getTitle();
                if (videoOpt.get().getUser() != null) {
                    targetOwnerId = videoOpt.get().getUser().getId();
                    targetOwnerUsername = videoOpt.get().getUser().getUsername();
                }
            }
        } else if (report.getTargetType() == ReportTargetType.COMMENT) {
            Optional<Comment> commentOpt = commentRepository.findById(report.getTargetId());
            if (commentOpt.isPresent()) {
                targetTitle = commentOpt.get().getContent();
                if (commentOpt.get().getUser() != null) {
                    targetOwnerId = commentOpt.get().getUser().getId();
                    targetOwnerUsername = commentOpt.get().getUser().getUsername();
                }
            }
        } else if (report.getTargetType() == ReportTargetType.USER) {
            Optional<User> userOpt = userRepository.findById(report.getTargetId());
            if (userOpt.isPresent()) {
                targetTitle = userOpt.get().getUsername();
                targetOwnerId = userOpt.get().getId();
                targetOwnerUsername = userOpt.get().getUsername();
            }
        }

        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterUsername(report.getReporter() != null ? report.getReporter().getUsername() : null)
                .reporterAvatarUrl(report.getReporter() != null ? report.getReporter().getAvatarUrl() : null)
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(targetTitle)
                .targetOwnerId(targetOwnerId)
                .targetOwnerUsername(targetOwnerUsername)
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .resolvedById(report.getResolvedBy() != null ? report.getResolvedBy().getId() : null)
                .resolvedByUsername(report.getResolvedBy() != null ? report.getResolvedBy().getUsername() : null)
                .resolutionNote(report.getResolutionNote())
                .actionTaken(report.getActionTaken())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
