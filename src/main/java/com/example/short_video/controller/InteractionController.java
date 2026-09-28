package com.example.short_video.controller;

import com.example.short_video.dto.request.CommentRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.CommentResponse;
import com.example.short_video.dto.response.LikeResponse;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.CommentService;
import com.example.short_video.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos/{videoId}")
@RequiredArgsConstructor
public class InteractionController {

    private final LikeService likeService;
    private final CommentService commentService;
    @PostMapping("/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
            @PathVariable Long videoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        LikeResponse response = likeService.toggleLike(videoId, userDetails.getUser());
        String message = response.isLiked() ? "Đã thích video" : "Đã bỏ thích video";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/like/status")
    public ResponseEntity<ApiResponse<LikeResponse>> getLikeStatus(
            @PathVariable Long videoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        LikeResponse response = likeService.getLikeStatus(videoId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái like thành công", response));
    }
    @PostMapping("/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long videoId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        CommentResponse response = commentService.addComment(videoId, request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bình luận thành công", response));
    }

    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CommentResponse> comments = commentService.getComments(videoId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bình luận thành công", comments));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getReplies(
            @PathVariable Long videoId,
            @PathVariable Long commentId) {
        List<CommentResponse> replies = commentService.getReplies(commentId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách trả lời thành công", replies));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long videoId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        commentService.deleteComment(commentId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Xóa bình luận thành công"));
    }

    private void requireAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
    }
}
