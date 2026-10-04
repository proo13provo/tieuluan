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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchHistoryServiceTest {

    @Mock
    private WatchHistoryRepository watchHistoryRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private WatchHistoryService watchHistoryService;

    private User currentUser;
    private User ownerUser;
    private Video sampleVideo;
    private VideoResponse sampleVideoResponse;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        ownerUser = User.builder()
                .id(2L)
                .username("creator")
                .email("creator@example.com")
                .build();

        sampleVideo = Video.builder()
                .id(100L)
                .title("Sample Short Video")
                .publicId("v12345")
                .views(10L)
                .duration(60.0)
                .user(ownerUser)
                .build();

        sampleVideoResponse = VideoResponse.builder()
                .id(100L)
                .title("Sample Short Video")
                .publicId("v12345")
                .views(11L)
                .duration(60.0)
                .userId(2L)
                .username("creator")
                .build();
    }

    @Test
    @DisplayName("recordWatchHistory: Ghi nhận xem video lần đầu và tăng views khi người xem không phải tác giả")
    void recordWatchHistory_newEntry_byViewer() {
        when(videoRepository.findById(100L)).thenReturn(Optional.of(sampleVideo));
        when(watchHistoryRepository.findByUserAndVideo(currentUser, sampleVideo)).thenReturn(Optional.empty());
        when(watchHistoryRepository.save(any(WatchHistory.class))).thenAnswer(invocation -> {
            WatchHistory h = invocation.getArgument(0);
            h.setId(1L);
            return h;
        });
        when(likeRepository.existsByVideoAndUser(sampleVideo, currentUser)).thenReturn(false);
        when(videoMapper.toVideoResponse(sampleVideo)).thenReturn(sampleVideoResponse);

        RecordWatchHistoryRequest request = RecordWatchHistoryRequest.builder()
                .watchedDuration(30.0)
                .completed(false)
                .build();

        WatchHistoryResponse response = watchHistoryService.recordWatchHistory(100L, request, currentUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getVideoId());
        assertEquals(1, response.getWatchCount());
        assertEquals(30.0, response.getWatchedDuration());
        assertFalse(response.getCompleted());
        assertEquals(11L, sampleVideo.getViews());

        verify(watchHistoryRepository).save(any(WatchHistory.class));
        verify(videoRepository).save(sampleVideo);
    }

    @Test
    @DisplayName("recordWatchHistory: Vẫn lưu lịch sử xem nhưng KHÔNG tăng view khi chính tác giả xem video của mình")
    void recordWatchHistory_byOwner_doesNotIncrementViews() {
        when(videoRepository.findById(100L)).thenReturn(Optional.of(sampleVideo));
        when(watchHistoryRepository.findByUserAndVideo(ownerUser, sampleVideo)).thenReturn(Optional.empty());
        when(watchHistoryRepository.save(any(WatchHistory.class))).thenAnswer(invocation -> {
            WatchHistory h = invocation.getArgument(0);
            h.setId(2L);
            return h;
        });
        when(likeRepository.existsByVideoAndUser(sampleVideo, ownerUser)).thenReturn(false);
        when(videoMapper.toVideoResponse(sampleVideo)).thenReturn(sampleVideoResponse);

        RecordWatchHistoryRequest request = RecordWatchHistoryRequest.builder()
                .watchedDuration(10.0)
                .completed(false)
                .build();

        WatchHistoryResponse response = watchHistoryService.recordWatchHistory(100L, request, ownerUser);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        // Lượt view vẫn giữ nguyên là 10, không bị tăng lên
        assertEquals(10L, sampleVideo.getViews());

        verify(watchHistoryRepository).save(any(WatchHistory.class));
        // videoRepository.save không được gọi khi là owner
        verify(videoRepository, never()).save(sampleVideo);
    }

    @Test
    @DisplayName("recordWatchHistory: Cập nhật lịch sử khi xem lại video")
    void recordWatchHistory_existingEntry() {
        WatchHistory existingHistory = WatchHistory.builder()
                .id(5L)
                .user(currentUser)
                .video(sampleVideo)
                .watchCount(2)
                .watchedDuration(15.0)
                .completed(false)
                .lastWatchedAt(Instant.now().minusSeconds(3600))
                .build();

        when(videoRepository.findById(100L)).thenReturn(Optional.of(sampleVideo));
        when(watchHistoryRepository.findByUserAndVideo(currentUser, sampleVideo)).thenReturn(Optional.of(existingHistory));
        when(watchHistoryRepository.save(existingHistory)).thenReturn(existingHistory);
        when(likeRepository.existsByVideoAndUser(sampleVideo, currentUser)).thenReturn(true);
        when(videoMapper.toVideoResponse(sampleVideo)).thenReturn(sampleVideoResponse);

        RecordWatchHistoryRequest request = RecordWatchHistoryRequest.builder()
                .watchedDuration(60.0)
                .completed(true)
                .build();

        WatchHistoryResponse response = watchHistoryService.recordWatchHistory(100L, request, currentUser);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals(3, response.getWatchCount());
        assertEquals(60.0, response.getWatchedDuration());
        assertTrue(response.getCompleted());
        assertEquals(11L, sampleVideo.getViews());

        verify(watchHistoryRepository).save(existingHistory);
        verify(videoRepository).save(sampleVideo);
    }

    @Test
    @DisplayName("recordWatchHistory: Quăng ngoại lệ khi không tìm thấy video")
    void recordWatchHistory_videoNotFound() {
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                watchHistoryService.recordWatchHistory(999L, null, currentUser));

        verify(watchHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("getWatchHistory: Lấy danh sách lịch sử xem có phân trang")
    void getWatchHistory_success() {
        WatchHistory h1 = WatchHistory.builder()
                .id(1L)
                .user(currentUser)
                .video(sampleVideo)
                .watchCount(1)
                .watchedDuration(20.0)
                .completed(false)
                .lastWatchedAt(Instant.now())
                .build();

        Page<WatchHistory> pagedHistories = new PageImpl<>(List.of(h1), PageRequest.of(0, 10), 1);
        when(watchHistoryRepository.findByUserOrderByLastWatchedAtDesc(eq(currentUser), any(PageRequest.class)))
                .thenReturn(pagedHistories);
        when(videoMapper.toVideoResponse(sampleVideo)).thenReturn(sampleVideoResponse);
        when(likeRepository.existsByVideoAndUser(sampleVideo, currentUser)).thenReturn(false);

        Page<WatchHistoryResponse> result = watchHistoryService.getWatchHistory(currentUser, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getVideoId());
    }

    @Test
    @DisplayName("deleteWatchHistoryItem: Xóa video khỏi lịch sử xem")
    void deleteWatchHistoryItem_success() {
        when(videoRepository.findById(100L)).thenReturn(Optional.of(sampleVideo));

        watchHistoryService.deleteWatchHistoryItem(100L, currentUser);

        verify(watchHistoryRepository).deleteByUserAndVideoId(currentUser, 100L);
    }

    @Test
    @DisplayName("clearWatchHistory: Xóa toàn bộ lịch sử xem")
    void clearWatchHistory_success() {
        when(watchHistoryRepository.deleteAllByUser(currentUser)).thenReturn(5);

        int count = watchHistoryService.clearWatchHistory(currentUser);

        assertEquals(5, count);
        verify(watchHistoryRepository).deleteAllByUser(currentUser);
    }
}
