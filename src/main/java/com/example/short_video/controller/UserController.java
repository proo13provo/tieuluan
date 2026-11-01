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
import java.util.Map;
import org.springframework.data.domain.Page;

import com.example.short_video.dto.request.BatchFollowStatusRequest;
import com.example.short_video.dto.response.FollowStatusResponse;
import com.example.short_video.dto.response.UserSummaryResponse;
import com.example.short_video.entity.User;
import com.example.short_video.service.FollowService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/user", "/api/users"})
@RequiredArgsConstructor
@Tag(name = "Người dùng & Quan hệ (User & Follow)", description = "Các API thông tin cá nhân, cập nhật profile, follow, followers, following, bạn bè")
public class UserController {

    private final UserService userService;
    private final FollowService followService;

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
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = (userDetails != null) ? userDetails.getUser() : null;
        return ResponseEntity.ok(userService.getUserProfileById(userId, currentUser));
    }
    @GetMapping("/{userId}/videos")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getUserVideos(
            @PathVariable Long userId,
            @RequestParam(value = "status", required = false) VideoStatus status) {
        return ResponseEntity.ok(userService.getUserVideos(userId, status));
    }

    // ─── FOLLOW & MUTUAL FRIENDS ──────────────────────────────────────────

    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> followOrToggle(
            @PathVariable Long userId,
            @RequestParam(value = "action", required = false) String action,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }

        FollowStatusResponse response;
        String msg;
        if ("follow".equalsIgnoreCase(action)) {
            response = followService.followUser(userDetails.getUser(), userId);
            msg = "Đã theo dõi người dùng";
        } else if ("unfollow".equalsIgnoreCase(action)) {
            response = followService.unfollowUser(userDetails.getUser(), userId);
            msg = "Đã hủy theo dõi người dùng";
        } else {
            response = followService.toggleFollow(userDetails.getUser(), userId);
            msg = response.isFollowing() ? "Đã theo dõi người dùng" : "Đã hủy theo dõi người dùng";
        }

        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> unfollowUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        FollowStatusResponse response = followService.unfollowUser(userDetails.getUser(), userId);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy theo dõi người dùng", response));
    }

    @PostMapping("/{userId}/unfollow")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> unfollowUserPost(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        FollowStatusResponse response = followService.unfollowUser(userDetails.getUser(), userId);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy theo dõi người dùng", response));
    }

    @GetMapping("/{userId}/follow-status")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getFollowStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        FollowStatusResponse response = followService.getFollowStatus(currentUser, userId);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái theo dõi", response));
    }

    @GetMapping("/{sourceUserId}/follow-status/{targetUserId}")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getFollowStatusBetween(
            @PathVariable Long sourceUserId,
            @PathVariable Long targetUserId) {
        FollowStatusResponse response = followService.getFollowStatusBetween(sourceUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái theo dõi giữa hai người dùng", response));
    }

    @PostMapping("/follow-status/batch")
    public ResponseEntity<ApiResponse<Map<Long, FollowStatusResponse>>> getBatchFollowStatusPost(
            @Valid @RequestBody BatchFollowStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        Map<Long, FollowStatusResponse> response = followService.getBatchFollowStatus(currentUser, request.getTargetUserIds());
        return ResponseEntity.ok(ApiResponse.success("Danh sách trạng thái theo dõi hàng loạt", response));
    }

    @GetMapping("/follow-status/batch")
    public ResponseEntity<ApiResponse<Map<Long, FollowStatusResponse>>> getBatchFollowStatusGet(
            @RequestParam("targetUserIds") List<Long> targetUserIds,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        Map<Long, FollowStatusResponse> response = followService.getBatchFollowStatus(currentUser, targetUserIds);
        return ResponseEntity.ok(ApiResponse.success("Danh sách trạng thái theo dõi hàng loạt", response));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<UserSummaryResponse> followers = followService.getFollowers(
                userId,
                userDetails != null ? userDetails.getUser() : null,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Danh sách người theo dõi", followers));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<UserSummaryResponse> following = followService.getFollowing(
                userId,
                userDetails != null ? userDetails.getUser() : null,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Danh sách đang theo dõi", following));
    }

    @GetMapping("/me/followers")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getMyFollowers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        Page<UserSummaryResponse> followers = followService.getFollowers(
                userDetails.getUser().getId(),
                userDetails.getUser(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Danh sách người theo dõi của tôi", followers));
    }

    @GetMapping("/me/following")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getMyFollowing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        Page<UserSummaryResponse> following = followService.getFollowing(
                userDetails.getUser().getId(),
                userDetails.getUser(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Danh sách người tôi đang theo dõi", following));
    }

    @GetMapping("/me/friends")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getMyFriends(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        List<UserSummaryResponse> friends = followService.getMutualFriends(userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Danh sách bạn bè cùng theo dõi", friends));
    }

    @GetMapping("/me/friends/page")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getMyFriendsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        Page<UserSummaryResponse> friends = followService.getMutualFriends(userDetails.getUser(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Danh sách bạn bè cùng theo dõi", friends));
    }
}

