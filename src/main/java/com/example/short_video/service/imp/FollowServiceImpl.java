package com.example.short_video.service.imp;

import com.example.short_video.dto.response.FollowStatusResponse;
import com.example.short_video.dto.response.UserSummaryResponse;
import com.example.short_video.entity.Follow;
import com.example.short_video.entity.User;
import com.example.short_video.exception.AppException;
import com.example.short_video.repository.FollowRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FollowStatusResponse toggleFollow(User currentUser, Long targetUserId) {
        if (currentUser.getId().equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bạn không thể theo dõi chính mình");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + targetUserId));

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(currentUser, targetUser);

        boolean nowFollowing;
        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            nowFollowing = false;
            log.info("[Follow] User {} unfollowed User {}", currentUser.getUsername(), targetUser.getUsername());
        } else {
            Follow follow = Follow.builder()
                    .follower(currentUser)
                    .following(targetUser)
                    .build();
            followRepository.save(follow);
            nowFollowing = true;
            log.info("[Follow] User {} followed User {}", currentUser.getUsername(), targetUser.getUsername());
        }

        boolean isFollowedBy = followRepository.existsByFollowerAndFollowing(targetUser, currentUser);
        boolean isMutual = nowFollowing && isFollowedBy;
        long followerCount = followRepository.countByFollowing(targetUser);
        long followingCount = followRepository.countByFollower(targetUser);

        return FollowStatusResponse.builder()
                .targetUserId(targetUserId)
                .isFollowing(nowFollowing)
                .isFollowedBy(isFollowedBy)
                .isMutual(isMutual)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(User currentUser, Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + targetUserId));

        boolean isFollowing = followRepository.existsByFollowerAndFollowing(currentUser, targetUser);
        boolean isFollowedBy = followRepository.existsByFollowerAndFollowing(targetUser, currentUser);
        boolean isMutual = isFollowing && isFollowedBy;
        long followerCount = followRepository.countByFollowing(targetUser);
        long followingCount = followRepository.countByFollower(targetUser);

        return FollowStatusResponse.builder()
                .targetUserId(targetUserId)
                .isFollowing(isFollowing)
                .isFollowedBy(isFollowedBy)
                .isMutual(isMutual)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getMutualFriends(User currentUser) {
        List<User> friends = followRepository.findMutualFriends(currentUser);
        return friends.stream()
                .map(u -> UserSummaryResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .avatarUrl(u.getAvatarUrl())
                        .bio(u.getBio())
                        .isMutual(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areMutualFriends(User u1, User u2) {
        if (u1 == null || u2 == null || u1.getId().equals(u2.getId())) {
            return false;
        }
        return followRepository.areMutualFriends(u1, u2);
    }
}
