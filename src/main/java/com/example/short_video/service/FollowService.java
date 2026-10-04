package com.example.short_video.service;

import com.example.short_video.dto.response.FollowStatusResponse;
import com.example.short_video.dto.response.UserSummaryResponse;
import com.example.short_video.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FollowService {

    FollowStatusResponse toggleFollow(User currentUser, Long targetUserId);

    FollowStatusResponse followUser(User currentUser, Long targetUserId);

    FollowStatusResponse unfollowUser(User currentUser, Long targetUserId);

    FollowStatusResponse getFollowStatus(User currentUser, Long targetUserId);

    Page<UserSummaryResponse> getFollowers(Long targetUserId, User currentUser, int page, int size);

    Page<UserSummaryResponse> getFollowing(Long targetUserId, User currentUser, int page, int size);

    List<UserSummaryResponse> getMutualFriends(User currentUser);

    Page<UserSummaryResponse> getMutualFriends(User currentUser, int page, int size);

    boolean areMutualFriends(User u1, User u2);
}
