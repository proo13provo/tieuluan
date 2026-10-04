package com.example.short_video.controller;

import com.example.short_video.dto.request.PublishVideoRequest;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.HashtagDto;
import com.example.short_video.dto.response.VideoUploadResponse;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
@Tag(name = "Video", description = "Các API tải lên video, xuất bản video, gợi ý hashtag")
public class VideoController {

    private final VideoService videoService;
    @PostMapping(value = "/upload-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoUploadResponse>> uploadVideo(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "hashtags", required = false) List<String> hashtags,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        VideoUploadResponse response = videoService.uploadVideo(
                file, title, hashtags, userDetails.getUser()
        );
        return ResponseEntity.ok(ApiResponse.success("Upload video thành công dưới dạng bản nháp (DRAFT)", response));
    }

    @RequestMapping(value = "/{videoId}/publish", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<VideoUploadResponse>> publishVideo(
            @PathVariable Long videoId,
            @Valid @RequestBody(required = false) PublishVideoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        VideoUploadResponse response = videoService.publishVideo(
                videoId, request, userDetails.getUser()
        );
        return ResponseEntity.ok(ApiResponse.success("Đăng video thành công", response));
    }

    @GetMapping("/hashtags/suggest")
    public ResponseEntity<ApiResponse<List<HashtagDto>>> suggestHashtags(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        List<HashtagDto> hashtags = videoService.suggestHashtags(query, limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gợi ý hashtag thành công", hashtags));
    }
}
