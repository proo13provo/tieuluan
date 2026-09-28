package com.example.short_video.controller;

import com.example.short_video.dto.request.SendLiveCommentRequest;
import com.example.short_video.dto.request.StartLiveRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.LiveCommentMessage;
import com.example.short_video.dto.response.LiveStreamResponse;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.LiveStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveStreamController {

    private final LiveStreamService liveStreamService;


    @PostMapping("/start")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> startLive(
            @Valid @RequestBody StartLiveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        LiveStreamResponse res = liveStreamService.startLive(request, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Bat dau live thanh cong", res));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> endLive(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        LiveStreamResponse res = liveStreamService.endLive(id, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Da ket thuc live", res));
    }

    // ─── FEED ──────────────────────────────────────────────────────────────

    @GetMapping({"", "/"})
    public ResponseEntity<ApiResponse<List<LiveStreamResponse>>> getLiveFeed() {
        return ResponseEntity.ok(ApiResponse.success("Danh sach live", liveStreamService.getLiveFeed()));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<LiveStreamResponse>>> getLiveFeedAlias() {
        return getLiveFeed();
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LiveStreamResponse>>> getActiveLives() {
        return getLiveFeed();
    }

    // ─── MY HISTORY ─────────────────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LiveStreamResponse>>> getMyStreams(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Lich su live", liveStreamService.getMyStreams(userDetails.getUser())));
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> getLiveByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Live stream cua nguoi dung", liveStreamService.getLiveByUser(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> getLiveById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var currentUser = userDetails != null ? userDetails.getUser() : null;
        return ResponseEntity.ok(ApiResponse.success("Chi tiet phien live", liveStreamService.getLiveById(id, currentUser)));
    }
    

    @PostMapping("/{id}/comment")
    public ResponseEntity<ApiResponse<LiveCommentMessage>> sendComment(
            @PathVariable Long id,
            @Valid @RequestBody SendLiveCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        LiveCommentMessage comment = liveStreamService.sendComment(id, request, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Gui binh luan thanh cong", comment));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<LiveCommentMessage>>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Danh sach binh luan", liveStreamService.getComments(id)));
    }

    // ─── VIEWER COUNT (WebSocket events trigger by service, REST for manual call) ─

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinLive(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        liveStreamService.incrementViewerCount(id);
        return ResponseEntity.ok(ApiResponse.success("Tham gia phong live"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveLive(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        liveStreamService.decrementViewerCount(id);
        return ResponseEntity.ok(ApiResponse.success("Roi khoi phong live"));
    }

    // ─── HELPER ────────────────────────────────────────────────────────────

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Ban chua dang nhap");
        }
    }
}
