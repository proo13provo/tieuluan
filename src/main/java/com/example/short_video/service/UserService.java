package com.example.short_video.service;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.request.UpdateBioRequest;
import com.example.short_video.dto.request.UpdateProfileRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final CloudinaryService cloudinaryService;
    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        return ApiResponse.success("Lấy thông tin người dùng thành công", buildUserProfile(user));
    }

    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getUserProfileById(Long userId) {
        return getUserProfileById(userId, null);
    }

    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getUserProfileById(Long userId, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        UserProfileResponse response = buildUserProfile(user);
        if (currentUser != null && !currentUser.getId().equals(userId)) {
            boolean isFollowing = followRepository.existsByFollowerAndFollowing(currentUser, user);
            boolean isFollowedBy = followRepository.existsByFollowerAndFollowing(user, currentUser);
            response.setIsFollowing(isFollowing);
            response.setIsFollowedBy(isFollowedBy);
            response.setIsMutual(isFollowing && isFollowedBy);
        } else if (currentUser != null && currentUser.getId().equals(userId)) {
            response.setIsFollowing(false);
            response.setIsFollowedBy(false);
            response.setIsMutual(false);
        }
        return ApiResponse.success("Lấy thông tin người dùng thành công", response);
    }
    @Transactional(readOnly = true)
    public ApiResponse<List<VideoResponse>> getMyVideos(String email, VideoStatus status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<Video> videos = findUserVideos(user, status);

        List<VideoResponse> videoResponses = videos.stream()
                .map(videoMapper::toVideoResponse)
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách video thành công", videoResponses);
    }
    @Transactional(readOnly = true)
    public ApiResponse<List<VideoResponse>> getUserVideos(Long userId, VideoStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        List<Video> videos = findUserVideos(user, status);
        List<VideoResponse> videoResponses = videos.stream()
                .map(videoMapper::toVideoResponse)
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách video thành công", videoResponses);
    }
    @Transactional
    public ApiResponse<UserProfileResponse> updateBio(String email, UpdateBioRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        String bio = request != null && request.getBio() != null ? request.getBio().trim() : null;
        user.setBio(bio);
        User savedUser = userRepository.save(user);

        return ApiResponse.success("Cập nhật tiểu sử thành công", buildUserProfile(savedUser));
    }

    @Transactional
    public ApiResponse<UserProfileResponse> updateAvatar(String email, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file hình ảnh đại diện");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ. Vui lòng chọn file ảnh (JPEG, PNG, WEBP,...)");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Dung lượng file ảnh không được vượt quá 10MB");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        UploadResult uploadResult = cloudinaryService.uploadImg(file);
        user.setAvatarUrl(uploadResult.getUrl());
        User savedUser = userRepository.save(user);

        return ApiResponse.success("Cập nhật ảnh đại diện thành công", buildUserProfile(savedUser));
    }

    @Transactional
    public ApiResponse<UserProfileResponse> updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (request != null) {
            if (request.getBio() != null) {
                user.setBio(request.getBio().trim());
            }
            if (request.getGender() != null) {
                user.setGender(request.getGender());
            }
        }
        User savedUser = userRepository.save(user);

        return ApiResponse.success("Cập nhật thông tin thành công", buildUserProfile(savedUser));
    }

    private UserProfileResponse buildUserProfile(User user) {
        long followerCount = followRepository.countByFollowing(user);
        long followingCount = followRepository.countByFollower(user);
        long totalLikes = likeRepository.countTotalLikesByVideoOwner(user);

        return userMapper.toUserProfileResponse(user, followerCount, followingCount, totalLikes);
    }
    private List<Video> findUserVideos(User user, VideoStatus status) {
        return (status != null)
                ? videoRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status)
                : videoRepository.findByUserOrderByCreatedAtDesc(user);
    }

}
