package com.example.short_video.controller;

import com.example.short_video.dto.request.CreateReportRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.ReportResponse;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Báo cáo vi phạm (Reports)", description = "API cho người dùng báo cáo vi phạm Video, Bình luận hoặc Tài khoản người dùng")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "Gửi báo cáo vi phạm", description = "Người dùng báo cáo vi phạm Video, Comment hoặc User kèm lý do và mô tả")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để gửi báo cáo vi phạm");
        }

        ApiResponse<ReportResponse> response = reportService.createReport(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
