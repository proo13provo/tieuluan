package com.example.short_video.service.imp;

import com.example.short_video.dto.response.FollowEventMessage;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

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

        if (nowFollowing) {
            sendFollowNotification(currentUser, targetUser, isMutual, followerCount);
        }

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
    @Transactional
    public FollowStatusResponse followUser(User currentUser, Long targetUserId) {
        if (currentUser.getId().equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bạn không thể theo dõi chính mình");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + targetUserId));

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(currentUser, targetUser);
        if (existingFollow.isEmpty()) {
            Follow follow = Follow.builder()
                    .follower(currentUser)
                    .following(targetUser)
                    .build();
            followRepository.save(follow);
            log.info("[Follow] User {} followed User {}", currentUser.getUsername(), targetUser.getUsername());
        }

        boolean isFollowedBy = followRepository.existsByFollowerAndFollowing(targetUser, currentUser);
        long followerCount = followRepository.countByFollowing(targetUser);
        long followingCount = followRepository.countByFollower(targetUser);
        boolean isMutual = isFollowedBy;

        if (existingFollow.isEmpty()) {
            sendFollowNotification(currentUser, targetUser, isMutual, followerCount);
        }

        return FollowStatusResponse.builder()
                .targetUserId(targetUserId)
                .isFollowing(true)
                .isFollowedBy(isFollowedBy)
                .isMutual(isMutual)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

    @Override
    @Transactional
    public FollowStatusResponse unfollowUser(User currentUser, Long targetUserId) {
        if (currentUser.getId().equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bạn không thể thao tác trên chính mình");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + targetUserId));

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(currentUser, targetUser);
        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            log.info("[Follow] User {} unfollowed User {}", currentUser.getUsername(), targetUser.getUsername());
        }

        boolean isFollowedBy = followRepository.existsByFollowerAndFollowing(targetUser, currentUser);
        long followerCount = followRepository.countByFollowing(targetUser);
        long followingCount = followRepository.countByFollower(targetUser);

        return FollowStatusResponse.builder()
                .targetUserId(targetUserId)
                .isFollowing(false)
                .isFollowedBy(isFollowedBy)
                .isMutual(false)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(User currentUser, Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + targetUserId));

        boolean isFollowing = currentUser != null && followRepository.existsByFollowerAndFollowing(currentUser, targetUser);
        boolean isFollowedBy = currentUser != null && followRepository.existsByFollowerAndFollowing(targetUser, currentUser);
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
    public Page<UserSummaryResponse> getFollowers(Long targetUserId, User currentUser, int page, int size) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + targetUserId));

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<User> followersPage = followRepository.findFollowersByUser(targetUser, pageable);

        return mapUsersToSummaryPage(followersPage, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getFollowing(Long targetUserId, User currentUser, int page, int size) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + targetUserId));

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<User> followingPage = followRepository.findFollowingByUser(targetUser, pageable);

        return mapUsersToSummaryPage(followingPage, currentUser);
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
                        .isFollowing(true)
                        .isFollowedBy(true)
                        .isMutual(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getMutualFriends(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<User> friends = followRepository.findMutualFriends(currentUser, pageable);
        return friends.map(u -> UserSummaryResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .avatarUrl(u.getAvatarUrl())
                .bio(u.getBio())
                .isFollowing(true)
                .isFollowedBy(true)
                .isMutual(true)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areMutualFriends(User u1, User u2) {
        if (u1 == null || u2 == null || u1.getId().equals(u2.getId())) {
            return false;
        }
        return followRepository.areMutualFriends(u1, u2);
    }

    private Page<UserSummaryResponse> mapUsersToSummaryPage(Page<User> usersPage, User currentUser) {
        List<User> users = usersPage.getContent();
        if (users.isEmpty()) {
            return usersPage.map(u -> null);
        }

        Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        Set<Long> followingIds = (currentUser != null && !userIds.isEmpty())
                ? followRepository.findFollowingIdsIn(currentUser, userIds)
                : Collections.emptySet();
        Set<Long> followerIds = (currentUser != null && !userIds.isEmpty())
                ? followRepository.findFollowerIdsIn(currentUser, userIds)
                : Collections.emptySet();

        return usersPage.map(u -> {
            boolean isFollowing = currentUser != null && followingIds.contains(u.getId());
            boolean isFollowedBy = currentUser != null && followerIds.contains(u.getId());
            boolean isMutual = isFollowing && isFollowedBy;
            return UserSummaryResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .avatarUrl(u.getAvatarUrl())
                    .bio(u.getBio())
                    .isFollowing(isFollowing)
                    .isFollowedBy(isFollowedBy)
                    .isMutual(isMutual)
                    .build();
        });
    }

    private void sendFollowNotification(User actor, User target, boolean isMutual, long followerCount) {
        try {
            FollowEventMessage event = FollowEventMessage.builder()
                    .type("FOLLOW")
                    .actorId(actor.getId())
                    .actorUsername(actor.getUsername())
                    .actorAvatarUrl(actor.getAvatarUrl())
                    .isMutual(isMutual)
                    .followerCount(followerCount)
                    .timestamp(Instant.now())
                    .build();
            messagingTemplate.convertAndSend("/topic/user/" + target.getId() + "/notifications", event);
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo follow qua WebSocket: {}", e.getMessage());
        }
    }
}
