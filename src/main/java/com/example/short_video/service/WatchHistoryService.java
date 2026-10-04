package com.example.short_video.service;

import com.example.short_video.dto.request.RecordWatchHistoryRequest;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.dto.response.WatchHistoryResponse;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.entity.WatchHistory;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.mapper.VideoMapper;
import com.example.short_video.repository.LikeRepository;
import com.example.short_video.repository.VideoRepository;
import com.example.short_video.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoRepository videoRepository;
    private final LikeRepository likeRepository;
    private final VideoMapper videoMapper;

    @Transactional
    public WatchHistoryResponse recordWatchHistory(Long videoId, RecordWatchHistoryRequest request, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));

        Instant now = Instant.now();
        WatchHistory history = watchHistoryRepository.findByUserAndVideo(user, video).orElse(null);

        if (history != null) {
            history.setLastWatchedAt(now);
            history.setWatchCount((history.getWatchCount() != null ? history.getWatchCount() : 0) + 1);
            if (request != null && request.getWatchedDuration() != null) {
                history.setWatchedDuration(request.getWatchedDuration());
            }
            if (request != null && Boolean.TRUE.equals(request.getCompleted())) {
                history.setCompleted(true);
            }
        } else {
            history = WatchHistory.builder()
                    .user(user)
                    .video(video)
                    .watchedDuration(request != null ? request.getWatchedDuration() : null)
                    .completed(request != null && Boolean.TRUE.equals(request.getCompleted()))
                    .watchCount(1)
                    .lastWatchedAt(now)
                    .build();
        }

        WatchHistory savedHistory = watchHistoryRepository.save(history);

        // Chỉ tăng lượt xem (views) nếu người xem không phải là chính chủ video
        boolean isOwner = video.getUser() != null && user != null && video.getUser().getId().equals(user.getId());
        if (!isOwner) {
            video.setViews(video.getViews() + 1);
            videoRepository.save(video);
        }

        return mapToResponse(savedHistory, user);
    }

    @Transactional(readOnly = true)
    public Page<WatchHistoryResponse> getWatchHistory(User user, int page, int size) {
        int targetSize = (size > 0 && size <= 100) ? size : 20;
        int targetPage = Math.max(0, page);
        PageRequest pageRequest = PageRequest.of(targetPage, targetSize);

        Page<WatchHistory> histories = watchHistoryRepository.findByUserOrderByLastWatchedAtDesc(user, pageRequest);
        return histories.map(h -> mapToResponse(h, user));
    }

    @Transactional(readOnly = true)
    public WatchHistoryResponse getWatchStatus(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));

        return watchHistoryRepository.findByUserAndVideo(user, video)
                .map(h -> mapToResponse(h, user))
                .orElse(null);
    }

    @Transactional
    public void deleteWatchHistoryItem(Long videoId, User user) {
        videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));

        watchHistoryRepository.deleteByUserAndVideoId(user, videoId);
    }

    @Transactional
    public int clearWatchHistory(User user) {
        return watchHistoryRepository.deleteAllByUser(user);
    }

    private WatchHistoryResponse mapToResponse(WatchHistory history, User currentUser) {
        Video video = history.getVideo();
        boolean isLiked = false;
        if (currentUser != null && video != null) {
            isLiked = likeRepository.existsByVideoAndUser(video, currentUser);
        }

        VideoResponse videoResponse = null;
        if (video != null) {
            videoResponse = videoMapper.toVideoResponse(video);
            videoResponse.setLiked(isLiked);
        }

        return WatchHistoryResponse.builder()
                .id(history.getId())
                .videoId(video != null ? video.getId() : null)
                .video(videoResponse)
                .watchedDuration(history.getWatchedDuration())
                .completed(history.getCompleted())
                .watchCount(history.getWatchCount())
                .lastWatchedAt(history.getLastWatchedAt())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
