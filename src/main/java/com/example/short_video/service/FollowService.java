package com.example.short_video.service;

import com.example.short_video.dto.response.FollowStatusResponse;
import com.example.short_video.dto.response.UserSummaryResponse;
import com.example.short_video.entity.User;

import java.util.List;

public interface FollowService {

    FollowStatusResponse toggleFollow(User currentUser, Long targetUserId);

    FollowStatusResponse getFollowStatus(User currentUser, Long targetUserId);

    List<UserSummaryResponse> getMutualFriends(User currentUser);

    boolean areMutualFriends(User u1, User u2);
}
