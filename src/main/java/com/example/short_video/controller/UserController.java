package com.example.short_video.controller;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.request.UpdateBioRequest;
import com.example.short_video.dto.request.UpdateProfileRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.UserProfileResponse;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        return ResponseEntity.ok(userService.getCurrentUserProfile(userDetails.getUsername()));
    }

    @PutMapping("/me/bio")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateBio(
            @Valid @RequestBody UpdateBioRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        return ResponseEntity.ok(userService.updateBio(userDetails.getUsername(), request));
    }

    @RequestMapping(value = "/me/avatar", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateAvatar(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "avatar", required = false) MultipartFile avatarFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        MultipartFile targetFile = (file != null && !file.isEmpty()) ? file : avatarFile;
        return ResponseEntity.ok(userService.updateAvatar(userDetails.getUsername(), targetFile));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), request));
    }
    @GetMapping("/me/videos")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getMyVideos(
            @RequestParam(value = "status", required = false) VideoStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        return ResponseEntity.ok(userService.getMyVideos(userDetails.getUsername(), status));
    }
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfileById(userId));
    }
    @GetMapping("/{userId}/videos")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getUserVideos(
            @PathVariable Long userId,
            @RequestParam(value = "status", required = false) VideoStatus status) {
        return ResponseEntity.ok(userService.getUserVideos(userId, status));
    }
}

