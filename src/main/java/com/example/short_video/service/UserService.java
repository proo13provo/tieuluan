package com.example.short_video.service;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.response.*;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.mapper.UserMapper;
import com.example.short_video.mapper.VideoMapper;
import com.example.short_video.repository.FollowRepository;
import com.example.short_video.repository.LikeRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final FollowRepository followRepository;
    private final LikeRepository likeRepository;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;

    /**
     * Lấy thông tin profile của người dùng hiện tại (đã đăng nhập).
     * Bao gồm: id, username, bio, avatarUrl, follower, following, totalLikes.
     */
    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        log.info("Lấy profile thành công cho user: {}", user.getEmail());
        return ApiResponse.success("Lấy thông tin người dùng thành công", buildUserProfile(user));
    }

    /**
     * Lấy thông tin profile của một user theo userId.
     * Dùng cho: xem profile người khác.
     */
    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        return ApiResponse.success("Lấy thông tin người dùng thành công", buildUserProfile(user));
    }

    /**
     * Lấy danh sách video của người dùng hiện tại (đã đăng nhập) theo trạng thái (nếu status = null thì lấy tất cả).
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<VideoResponse>> getMyVideos(String email, VideoStatus status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<Video> videos = (status != null)
                ? videoRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status)
                : videoRepository.findByUserOrderByCreatedAtDesc(user);

        List<VideoResponse> videoResponses = videos.stream()
                .map(videoMapper::toVideoResponse)
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách video thành công", videoResponses);
    }

    /**
     * Lấy danh sách video của một user theo userId và trạng thái (nếu status = null thì lấy tất cả).
     * Dùng cho: xem video người khác hoặc chính mình.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<VideoResponse>> getUserVideos(Long userId, VideoStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<Video> videos = (status != null)
                ? videoRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status)
                : videoRepository.findByUserOrderByCreatedAtDesc(user);

        List<VideoResponse> videoResponses = videos.stream()
                .map(videoMapper::toVideoResponse)
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách video thành công", videoResponses);
    }

    /**
     * Build UserProfileResponse từ User entity (dùng chung cho /me và /{userId}/profile)
     */
    private UserProfileResponse buildUserProfile(User user) {
        long followerCount = followRepository.countByFollowing(user);
        long followingCount = followRepository.countByFollower(user);
        long totalLikes = likeRepository.countTotalLikesByVideoOwner(user);

        return userMapper.toUserProfileResponse(user, followerCount, followingCount, totalLikes);
    }
}
