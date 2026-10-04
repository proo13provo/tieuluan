package com.example.short_video.controller;

import com.example.short_video.dto.request.RecordWatchHistoryRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.WatchHistoryResponse;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.WatchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Lịch sử xem/nghe (Watch History)", description = "Các API ghi nhận lịch sử xem/nghe video, lấy danh sách lịch sử, và xóa lịch sử của người dùng")
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @Operation(summary = "Ghi nhận lịch sử xem/nghe video", description = "Lưu hoặc cập nhật thời gian xem gần nhất, số lần xem, và tăng view cho video")
    @PostMapping({"/api/videos/{videoId}/watch", "/api/history/videos/{videoId}"})
    public ResponseEntity<ApiResponse<WatchHistoryResponse>> recordWatch(
            @PathVariable Long videoId,
            @Valid @RequestBody(required = false) RecordWatchHistoryRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        WatchHistoryResponse response = watchHistoryService.recordWatchHistory(videoId, request, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận lịch sử xem video thành công", response));
    }

    @Operation(summary = "Lấy danh sách lịch sử xem/nghe của tôi", description = "Trả về danh sách video đã xem/nghe có phân trang, sắp xếp theo thời gian xem gần nhất")
    @GetMapping({"/api/user/history", "/api/history"})
    public ResponseEntity<ApiResponse<Page<WatchHistoryResponse>>> getWatchHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        Page<WatchHistoryResponse> response = watchHistoryService.getWatchHistory(userDetails.getUser(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch sử xem video thành công", response));
    }

    @Operation(summary = "Kiểm tra trạng thái xem/nghe của một video", description = "Kiểm tra xem người dùng đã từng xem video này chưa và tiến độ đã xem")
    @GetMapping({"/api/videos/{videoId}/watch-status", "/api/history/videos/{videoId}/status"})
    public ResponseEntity<ApiResponse<WatchHistoryResponse>> getWatchStatus(
            @PathVariable Long videoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        WatchHistoryResponse response = watchHistoryService.getWatchStatus(videoId, userDetails.getUser());
        String msg = response != null ? "Lấy thông tin xem video thành công" : "Người dùng chưa từng xem video này";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    @Operation(summary = "Xóa một video khỏi lịch sử xem", description = "Xóa video được chỉ định khỏi danh sách lịch sử của người dùng hiện tại")
    @DeleteMapping({"/api/user/history/{videoId}", "/api/history/videos/{videoId}"})
    public ResponseEntity<ApiResponse<Void>> deleteHistoryItem(
            @PathVariable Long videoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        watchHistoryService.deleteWatchHistoryItem(videoId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Xóa video khỏi lịch sử xem thành công"));
    }

    @Operation(summary = "Xóa toàn bộ lịch sử xem", description = "Xóa toàn bộ danh sách lịch sử xem của người dùng hiện tại")
    @DeleteMapping({"/api/user/history", "/api/history"})
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        int deletedCount = watchHistoryService.clearWatchHistory(userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ lịch sử xem thành công (" + deletedCount + " video)"));
    }

    private void requireAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
    }
}
