package com.example.short_video.service;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.request.PublishVideoRequest;
import com.example.short_video.dto.response.HashtagDto;
import com.example.short_video.dto.response.UploadResult;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.dto.response.VideoUploadResponse;
import com.example.short_video.entity.Category;
import com.example.short_video.entity.Hashtag;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.mapper.HashtagMapper;
import com.example.short_video.mapper.VideoMapper;
import com.example.short_video.repository.HashtagRepository;
import com.example.short_video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final HashtagRepository hashtagRepository;
    private final CloudinaryService cloudinaryService;
    private final AIservice aiService;
    private final VideoMapper videoMapper;
    private final HashtagMapper hashtagMapper;

    @Transactional
    public VideoUploadResponse uploadVideo(MultipartFile file, String title, List<String> hashtags, User user) throws IOException {
        UploadResult uploadResult = cloudinaryService.uploadVideo(file);
        Set<Hashtag> hashtagEntities = processHashtags(hashtags);
        Set<Category> categoryEntities = new HashSet<>();
        if (title != null && !title.isBlank()) {
            categoryEntities = aiService.categorizeVideo(title, hashtags);
        }
        VideoStatus targetStatus = VideoStatus.DRAFT;
        Video video = Video.builder()
                .publicId(uploadResult.getPublicId())
                .title(title)
                .user(user)
                .duration(uploadResult.getDuration())
                .views(0)
                .shares(0L)
                .hashtags(hashtagEntities)
                .categories(categoryEntities)
                .status(targetStatus)
                .iscomment(true)
                .build();
        Video savedVideo = videoRepository.save(video);
        return videoMapper.toVideoUploadResponse(savedVideo);
    }

    @Transactional
    public VideoUploadResponse publishVideo(Long videoId, PublishVideoRequest request, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));
        if (!video.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa hoặc đăng video này");
        }
        if (request != null && request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        List<String> requestHashtags = (request != null) ? request.getHashtags() : null;
        if (requestHashtags != null) {
            Set<Hashtag> hashtagEntities = processHashtags(requestHashtags);
            video.setHashtags(hashtagEntities);
        }
        if (request != null && request.getIsComment() != null) {
            video.setIscomment(request.getIsComment());
        }
        Set<Category> categoryEntities = aiService.categorizeVideo(video.getTitle(), requestHashtags);
        if (categoryEntities != null && !categoryEntities.isEmpty()) {
            video.setCategories(categoryEntities);
        }
        video.setStatus(VideoStatus.PUBLISHED);
        Video updatedVideo = videoRepository.save(video);
        return videoMapper.toVideoUploadResponse(updatedVideo);
    }
    @Transactional(readOnly = true)
    public List<HashtagDto> suggestHashtags(String query, int limit) {
        int targetLimit = (limit > 0 && limit <= 50) ? limit : 10;
        PageRequest pageRequest = PageRequest.of(0, targetLimit);

        if (query == null || query.isBlank()) {
            return hashtagMapper.toDtoList(hashtagRepository.findAllByOrderByUsageCountDesc(pageRequest));
        }

        String normalizedQuery = query.trim().toLowerCase();
        if (normalizedQuery.startsWith("#")) {
            normalizedQuery = normalizedQuery.substring(1).trim();
        }

        if (normalizedQuery.isEmpty()) {
            return hashtagMapper.toDtoList(hashtagRepository.findAllByOrderByUsageCountDesc(pageRequest));
        }

        List<Hashtag> hashtags = hashtagRepository.findByNameStartingWithIgnoreCaseOrderByUsageCountDesc(
                normalizedQuery, pageRequest
        );
        if (hashtags.size() < targetLimit) {
            int remaining = targetLimit - hashtags.size();
            List<Hashtag> containing = hashtagRepository.findByNameContainingIgnoreCaseOrderByUsageCountDesc(
                    normalizedQuery, PageRequest.of(0, targetLimit)
            );
            for (Hashtag h : containing) {
                if (!hashtags.contains(h)) {
                    hashtags.add(h);
                    if (hashtags.size() >= targetLimit) break;
                }
            }
        }

        return hashtagMapper.toDtoList(hashtags);
    }
    @Transactional(readOnly = true)
    public List<com.example.short_video.dto.response.VideoResponse> getVideosByUserAndStatus(User user, VideoStatus status) {
        List<Video> videos = (status != null)
                ? videoRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status)
                : videoRepository.findByUserOrderByCreatedAtDesc(user);

        return videos.stream()
                .map(videoMapper::toVideoResponse)
                .toList();
    }
    private Set<Hashtag> processHashtags(List<String> hashtags) {
        Set<Hashtag> hashtagEntities = new HashSet<>();
        if (hashtags != null && !hashtags.isEmpty()) {
            for (String tagName : hashtags) {
                if (tagName == null) continue;
                String normalizedName = tagName.trim().toLowerCase();
                if (normalizedName.startsWith("#")) {
                    normalizedName = normalizedName.substring(1);
                }
                if (normalizedName.isEmpty()) continue;
                String finalName = normalizedName;
                Hashtag hashtag = hashtagRepository.findByName(finalName)
                        .orElseGet(() -> hashtagRepository.save(new Hashtag(finalName)));
                hashtag.setUsageCount(hashtag.getUsageCount() + 1);
                hashtagEntities.add(hashtag);
            }
        }
        return hashtagEntities;
    }
}
