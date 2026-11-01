package com.example.short_video.service;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.request.AiRecommendRequest;
import com.example.short_video.dto.response.AiRecommendResponse;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.entity.Category;
import com.example.short_video.entity.Hashtag;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.entity.WatchHistory;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.mapper.VideoMapper;
import com.example.short_video.repository.CommentRepository;
import com.example.short_video.repository.LikeRepository;
import com.example.short_video.repository.VideoRepository;
import com.example.short_video.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final VideoRepository videoRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final VideoMapper videoMapper;
    private final AiRecommendationClient aiRecommendationClient;

    @Transactional(readOnly = true)
    public List<VideoResponse> getRecommendedFeed(User user, int page, int size) {
        int targetPage = Math.max(0, page);
        int targetSize = (size > 0 && size <= 50) ? size : 10;

        List<Video> candidateVideos = videoRepository.findByStatus(VideoStatus.PUBLISHED);
        if (candidateVideos == null || candidateVideos.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Video> videoMap = candidateVideos.stream()
                .collect(Collectors.toMap(Video::getId, v -> v, (v1, v2) -> v1));
        Set<Long> likedVideoIds = (user != null)
                ? new HashSet<>(likeRepository.findLikedVideoIdsByUser(user))
                : Collections.emptySet();
        List<AiRecommendRequest.CandidateVideoDto> candidateDtos = candidateVideos.stream().map(v -> {
            List<String> catNames = v.getCategories() != null
                    ? v.getCategories().stream().map(Category::getName).toList()
                    : Collections.emptyList();
            List<String> tagNames = v.getHashtags() != null
                    ? v.getHashtags().stream().map(Hashtag::getName).toList()
                    : Collections.emptyList();

            return AiRecommendRequest.CandidateVideoDto.builder()
                    .id(v.getId())
                    .title(v.getTitle())
                    .views(v.getViews())
                    .likes(v.getLikes() != null ? v.getLikes() : 0L)
                    .shares(v.getShares() != null ? v.getShares() : 0L)
                    .duration(v.getDuration())
                    .createdAt(v.getCreatedAt() != null ? v.getCreatedAt().toString() : null)
                    .categories(catNames)
                    .hashtags(tagNames)
                    .build();
        }).toList();
        List<AiRecommendRequest.WatchHistoryItemDto> historyDtos = new ArrayList<>();
        if (user != null) {
            Set<Long> commentedVideoIds = new HashSet<>(commentRepository.findCommentedVideoIdsByUser(user));
            List<WatchHistory> histories = watchHistoryRepository.findTop100ByUserOrderByLastWatchedAtDesc(user);

            for (WatchHistory wh : histories) {
                Video v = wh.getVideo();
                if (v == null) continue;

                List<String> catNames = v.getCategories() != null
                        ? v.getCategories().stream().map(Category::getName).toList()
                        : Collections.emptyList();
                List<String> tagNames = v.getHashtags() != null
                        ? v.getHashtags().stream().map(Hashtag::getName).toList()
                        : Collections.emptyList();

                Double watchedDuration = wh.getWatchedDuration();
                Double duration = v.getDuration();
                boolean isCompleted = Boolean.TRUE.equals(wh.getCompleted());
                boolean isSkipped = (watchedDuration != null && duration != null && duration >= 5.0 && watchedDuration < 3.0 && !isCompleted);

                historyDtos.add(AiRecommendRequest.WatchHistoryItemDto.builder()
                        .videoId(v.getId())
                        .watchedDuration(watchedDuration)
                        .duration(duration)
                        .completed(isCompleted)
                        .watchCount(wh.getWatchCount())
                        .isLiked(likedVideoIds.contains(v.getId()))
                        .isCommented(commentedVideoIds.contains(v.getId()))
                        .isShared(false)
                        .isSkipped(isSkipped)
                        .lastWatchedAt(wh.getLastWatchedAt() != null ? wh.getLastWatchedAt().toString() : null)
                        .categories(catNames)
                        .hashtags(tagNames)
                        .build());
            }
        }
        AiRecommendRequest aiRequest = AiRecommendRequest.builder()
                .userId(user != null ? user.getId() : null)
                .page(targetPage)
                .limit(targetSize)
                .candidateVideos(candidateDtos)
                .watchHistory(historyDtos)
                .build();
        AiRecommendResponse aiResponse = aiRecommendationClient.getRecommendations(aiRequest);
        List<VideoResponse> resultList = new ArrayList<>();
        if (aiResponse != null && aiResponse.getVideos() != null && !aiResponse.getVideos().isEmpty()) {
            log.info("Nhận được đề xuất từ AI microservice (Chiến lược: {}, Số lượng: {})",
                    aiResponse.getStrategy(), aiResponse.getVideos().size());

            for (Map<String, Object> item : aiResponse.getVideos()) {
                Object idObj = item.get("id");
                if (idObj == null) continue;
                Long vid = Long.valueOf(idObj.toString());
                Video video = videoMap.get(vid);
                if (video != null) {
                    VideoResponse res = videoMapper.toVideoResponse(video);
                    res.setLiked(likedVideoIds.contains(vid));
                    resultList.add(res);
                }
            }
        } else {
            log.warn("Sử dụng Fallback Ranking do AI service không phản hồi");
            resultList = fallbackRanking(candidateVideos, likedVideoIds, targetPage, targetSize);
        }

        return resultList;
    }
    @Transactional
    public Long shareVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));

        videoRepository.incrementShares(videoId);
        return (video.getShares() != null ? video.getShares() : 0L) + 1L;
    }
    private List<VideoResponse> fallbackRanking(
            List<Video> videos,
            Set<Long> likedVideoIds,
            int page,
            int size
    ) {
        List<Video> sorted = new ArrayList<>(videos);
        sorted.sort((v1, v2) -> {
            long score1 = v1.getViews() + (v1.getLikes() != null ? v1.getLikes() : 0) * 2;
            long score2 = v2.getViews() + (v2.getLikes() != null ? v2.getLikes() : 0) * 2;
            return Long.compare(score2, score1);
        });

        int fromIndex = page * size;
        if (fromIndex >= sorted.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + size, sorted.size());

        return sorted.subList(fromIndex, toIndex).stream().map(v -> {
            VideoResponse res = videoMapper.toVideoResponse(v);
            res.setLiked(likedVideoIds.contains(v.getId()));
            return res;
        }).collect(Collectors.toList());
    }
}
