package com.example.short_video.service;

import com.example.short_video.Enum.ReportStatus;
import com.example.short_video.Enum.ReportTargetType;
import com.example.short_video.dto.request.CreateReportRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.ReportResponse;
import com.example.short_video.entity.Comment;
import com.example.short_video.entity.Report;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.exception.ResourceAlreadyExistsException;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.repository.CommentRepository;
import com.example.short_video.repository.ReportRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApiResponse<ReportResponse> createReport(CreateReportRequest request, User reporter) {
        String targetTitle = null;
        Long targetOwnerId = null;
        String targetOwnerUsername = null;

        if (request.getTargetType() == ReportTargetType.VIDEO) {
            Video video = videoRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với ID: " + request.getTargetId()));
            targetTitle = video.getTitle();
            if (video.getUser() != null) {
                targetOwnerId = video.getUser().getId();
                targetOwnerUsername = video.getUser().getUsername();
            }
        } else if (request.getTargetType() == ReportTargetType.COMMENT) {
            Comment comment = commentRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với ID: " + request.getTargetId()));
            targetTitle = comment.getContent();
            if (comment.getUser() != null) {
                targetOwnerId = comment.getUser().getId();
                targetOwnerUsername = comment.getUser().getUsername();
            }
        } else if (request.getTargetType() == ReportTargetType.USER) {
            User targetUser = userRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + request.getTargetId()));
            targetTitle = targetUser.getUsername();
            targetOwnerId = targetUser.getId();
            targetOwnerUsername = targetUser.getUsername();
        }

        if (reportRepository.existsByReporterAndTargetTypeAndTargetId(reporter, request.getTargetType(), request.getTargetId())) {
            throw new ResourceAlreadyExistsException("Bạn đã gửi báo cáo cho nội dung này trước đó rồi.");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        Report saved = reportRepository.save(report);

        ReportResponse response = ReportResponse.builder()
                .id(saved.getId())
                .reporterId(reporter.getId())
                .reporterUsername(reporter.getUsername())
                .reporterAvatarUrl(reporter.getAvatarUrl())
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .targetTitle(targetTitle)
                .targetOwnerId(targetOwnerId)
                .targetOwnerUsername(targetOwnerUsername)
                .reason(saved.getReason())
                .description(saved.getDescription())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();

        return ApiResponse.success("Gửi báo cáo thành công. Đội ngũ kiểm duyệt sẽ xem xét trong thời gian sớm nhất.", response);
    }
}
