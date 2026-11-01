package com.example.short_video.controller;

import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.UnifiedSearchResponse;
import com.example.short_video.entity.User;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tìm kiếm thông minh (Smart AI Search)", description = "API tìm kiếm thông minh gộp chung cả Người dùng và Video ứng dụng AI Vector Search (SentenceTransformer)")
public class SearchController {

    private final SearchService searchService;

    @Operation(
            summary = "Tìm kiếm thông minh gộp chung (Smart Unified Search - Users & Videos)",
            description = "Tìm kiếm đồng thời cả Người dùng (Users / Creators) và Video (Videos) trong một lần gọi API duy nhất:\n" +
                    "- Người dùng: Khớp tên người dùng (@username), đối sánh ngữ nghĩa tiểu sử Bio (SentenceTransformer), hiển thị số follower & trạng thái theo dõi.\n" +
                    "- Video: Khớp ngữ nghĩa AI, khớp từ vựng không dấu, chủ đề Categories & Hashtags, cân bằng độ phổ biến views/likes.\n" +
                    "- Tự động Fallback về cơ sở dữ liệu nếu AI microservice chưa sẵn sàng."
    )
    @GetMapping({"/api/search","/api/videos/search"})
    public ResponseEntity<ApiResponse<UnifiedSearchResponse>> search(
            @Parameter(description = "Từ khóa tìm kiếm (ví dụ: 'ẩm thực', 'chef', 'bong da', 'lập trình')", example = "nấu ăn ngon")
            @RequestParam(value = "q", required = false) String query,
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Số lượng mỗi trang (tối đa 50)", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "Tiêu chí sắp xếp video: RELEVANCE (độ liên quan AI), LATEST (mới nhất), POPULAR (phổ biến nhất)", example = "RELEVANCE")
            @RequestParam(value = "sortBy", defaultValue = "RELEVANCE") String sortBy,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User currentUser = (userDetails != null) ? userDetails.getUser() : null;
        UnifiedSearchResponse response = searchService.search(query, page, size, sortBy, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thông minh thành công", response));
    }
}
