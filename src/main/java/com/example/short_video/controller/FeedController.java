package com.example.short_video.controller;

import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.entity.User;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Bảng tin đề xuất (Feed Recommendation)", description = "Các API đề xuất video kết nối với AI microservice (Cold Start & Personalized Feed)")
public class FeedController {

    private final FeedService feedService;

    @Operation(
            summary = "Lấy danh sách video đề xuất (Feed)",
            description = "Gợi ý video được xếp hạng bởi AI microservice:\n" +
                    "- Người dùng mới / Khách chưa đăng nhập: Thuật toán Cold Start (score = 0.4*popularity + 0.3*recency + 0.2*engagement + 0.1*random)\n" +
                    "- Người dùng đã có lịch sử xem: Thuật toán Personalized (phân tích watch_time, completion_rate, like, comment, share, skip để suy ra hashtag & category yêu thích)"
    )
    @GetMapping({"/api/feed", "/api/videos/feed"})
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getFeed(
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng video mỗi trang (tối đa 50)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User currentUser = (userDetails != null) ? userDetails.getUser() : null;
        List<VideoResponse> feed = feedService.getRecommendedFeed(currentUser, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách video đề xuất thành công", feed));
    }

    @Operation(
            summary = "Chia sẻ video (Share)",
            description = "Tăng số lượt share của video và ghi nhận tín hiệu tương tác tích cực cho thuật toán gợi ý"
    )
    @PostMapping("/api/videos/{videoId}/share")
    public ResponseEntity<ApiResponse<Map<String, Object>>> shareVideo(@PathVariable Long videoId) {
        Long totalShares = feedService.shareVideo(videoId);
        return ResponseEntity.ok(ApiResponse.success("Chia sẻ video thành công", Map.of(
                "videoId", videoId,
                "shares", totalShares
        )));
    }
}
