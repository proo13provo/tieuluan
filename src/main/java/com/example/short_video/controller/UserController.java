package com.example.short_video.controller;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.UserProfileResponse;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Lấy thông tin profile của người dùng hiện tại (đã đăng nhập).
     * GET /api/user/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(userDetails.getUsername()));
    }

    /**
     * Lấy danh sách video của người dùng hiện tại theo trạng thái.
     * GET /api/user/me/videos?status=DRAFT (hoặc PUBLISHED, PRIVATE, ...)
     */
    @GetMapping("/me/videos")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getMyVideos(
            @RequestParam(value = "status", required = false) VideoStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        return ResponseEntity.ok(userService.getMyVideos(userDetails.getUsername(), status));
    }

    /**
     * Lấy thông tin profile của một user theo userId.
     * Dùng cho: xem trang cá nhân của người khác.
     * GET /api/user/{userId}/profile
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfileById(userId));
    }

    /**
     * Lấy danh sách video của một user theo userId và trạng thái (tùy chọn).
     * GET /api/user/{userId}/videos?status=PUBLISHED
     */
    @GetMapping("/{userId}/videos")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getUserVideos(
            @PathVariable Long userId,
            @RequestParam(value = "status", required = false) VideoStatus status) {
        return ResponseEntity.ok(userService.getUserVideos(userId, status));
    }
}

