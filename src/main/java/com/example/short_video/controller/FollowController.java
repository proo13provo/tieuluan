package com.example.short_video.controller;

import com.example.short_video.dto.request.BatchFollowStatusRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.FollowStatusResponse;
import com.example.short_video.entity.User;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/follow", "/api/follows"})
@RequiredArgsConstructor
@Tag(name = "Theo dõi (Follow)", description = "Các API kiểm tra trạng thái follow đơn lẻ, hàng loạt (batch) và thao tác follow/unfollow")
public class FollowController {

    private final FollowService followService;

    @GetMapping("/{targetUserId}/status")
    @Operation(summary = "Xem trạng thái follow của một người dùng", description = "Kiểm tra xem người dùng hiện tại có đang theo dõi targetUserId không, có được targetUserId theo dõi lại không, và lấy số lượng follower/following")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getStatus(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        FollowStatusResponse response = followService.getFollowStatus(currentUser, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái theo dõi", response));
    }

    @GetMapping("/status/{targetUserId}")
    @Operation(summary = "Xem trạng thái follow (Alias /status/{targetUserId})")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getStatusAlias(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return getStatus(targetUserId, userDetails);
    }

    @GetMapping("/status")
    @Operation(summary = "Xem trạng thái follow qua query param", description = "Hỗ trợ truyền targetUserId và tùy chọn sourceUserId (nếu không truyền sourceUserId sẽ lấy người dùng hiện tại)")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getStatusByParam(
            @RequestParam Long targetUserId,
            @RequestParam(required = false) Long sourceUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (sourceUserId != null) {
            FollowStatusResponse response = followService.getFollowStatusBetween(sourceUserId, targetUserId);
            return ResponseEntity.ok(ApiResponse.success("Trạng thái theo dõi giữa hai người dùng", response));
        }
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        FollowStatusResponse response = followService.getFollowStatus(currentUser, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái theo dõi", response));
    }

    @GetMapping("/status/between")
    @Operation(summary = "Xem trạng thái follow giữa 2 người dùng bất kỳ")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getStatusBetween(
            @RequestParam Long sourceUserId,
            @RequestParam Long targetUserId) {
        FollowStatusResponse response = followService.getFollowStatusBetween(sourceUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái theo dõi giữa hai người dùng", response));
    }

    @PostMapping({"/status/batch", "/batch-status"})
    @Operation(summary = "Xem trạng thái follow hàng loạt (POST)", description = "Nhận danh sách targetUserIds và trả về map trạng thái follow cho từng user (dùng khi render feed, danh sách tìm kiếm, người dùng)")
    public ResponseEntity<ApiResponse<Map<Long, FollowStatusResponse>>> getBatchStatusPost(
            @Valid @RequestBody BatchFollowStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        Map<Long, FollowStatusResponse> response = followService.getBatchFollowStatus(currentUser, request.getTargetUserIds());
        return ResponseEntity.ok(ApiResponse.success("Danh sách trạng thái theo dõi hàng loạt", response));
    }

    @GetMapping({"/status/batch", "/batch-status"})
    @Operation(summary = "Xem trạng thái follow hàng loạt (GET query param)", description = "Hỗ trợ truyền targetUserIds=1,2,3...")
    public ResponseEntity<ApiResponse<Map<Long, FollowStatusResponse>>> getBatchStatusGet(
            @RequestParam("targetUserIds") List<Long> targetUserIds,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        Map<Long, FollowStatusResponse> response = followService.getBatchFollowStatus(currentUser, targetUserIds);
        return ResponseEntity.ok(ApiResponse.success("Danh sách trạng thái theo dõi hàng loạt", response));
    }

    @PostMapping("/{targetUserId}")
    @Operation(summary = "Theo dõi hoặc hủy theo dõi người dùng (Toggle/Action)", description = "Query param action có thể là 'follow', 'unfollow', hoặc để trống để toggle")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> followUser(
            @PathVariable Long targetUserId,
            @RequestParam(required = false) String action,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện thao tác theo dõi");
        }
        FollowStatusResponse response;
        String msg;
        if ("follow".equalsIgnoreCase(action)) {
            response = followService.followUser(userDetails.getUser(), targetUserId);
            msg = "Đã theo dõi người dùng";
        } else if ("unfollow".equalsIgnoreCase(action)) {
            response = followService.unfollowUser(userDetails.getUser(), targetUserId);
            msg = "Đã hủy theo dõi người dùng";
        } else {
            response = followService.toggleFollow(userDetails.getUser(), targetUserId);
            msg = response.isFollowing() ? "Đã theo dõi người dùng" : "Đã hủy theo dõi người dùng";
        }
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    @DeleteMapping("/{targetUserId}")
    @Operation(summary = "Hủy theo dõi người dùng")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> unfollowUser(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện thao tác");
        }
        FollowStatusResponse response = followService.unfollowUser(userDetails.getUser(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy theo dõi người dùng", response));
    }
}
