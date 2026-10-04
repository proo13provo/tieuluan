package com.example.short_video.controller;

import com.example.short_video.dto.request.SendMessageRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.ChatMessageResponse;
import com.example.short_video.dto.response.ConversationResponse;
import com.example.short_video.dto.response.UserSummaryResponse;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.ChatService;
import com.example.short_video.service.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Nhắn tin (Chat)", description = "Các API nhắn tin trực tiếp giữa bạn bè cùng theo dõi (Mutual Follow)")
public class ChatController {

    private final ChatService chatService;
    private final FollowService followService;
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        ChatMessageResponse res = chatService.sendMessage(
                userDetails.getUser(), request.getReceiverId(), request.getContent());
        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công", res));
    }
    @PostMapping("/{targetUserId}/send")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessageToUser(
            @PathVariable Long targetUserId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        ChatMessageResponse res = chatService.sendMessage(
                userDetails.getUser(), targetUserId, request.getContent());
        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công", res));
    }

    @GetMapping("/history/{targetUserId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getHistory(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        Pageable pageable = PageRequest.of(page, size);
        List<ChatMessageResponse> history = chatService.getMessageHistory(
                userDetails.getUser(), targetUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lịch sử tin nhắn", history));
    }
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        List<ConversationResponse> conversations = chatService.getConversations(userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Danh sách cuộc trò chuyện", conversations));
    }
    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getMutualFriends(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        List<UserSummaryResponse> friends = followService.getMutualFriends(userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Danh sách bạn bè cùng theo dõi", friends));
    }
    @PutMapping("/read/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        chatService.markConversationAsRead(userDetails.getUser(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc"));
    }
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        long count = chatService.getTotalUnreadCount(userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Số tin nhắn chưa đọc", Map.of("unreadCount", count)));
    }
    @GetMapping("/can-chat/{targetUserId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCanChat(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        boolean canChat = chatService.canChat(userDetails.getUser(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra quyền nhắn tin", Map.of(
                "canChat", canChat,
                "targetUserId", targetUserId,
                "reason", canChat ? "Đủ điều kiện (hai người cùng theo dõi nhau)" : "Chưa đủ điều kiện (cần cùng theo dõi nhau)"
        )));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập để sử dụng tính năng nhắn tin");
        }
    }
}
