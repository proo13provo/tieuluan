package com.example.short_video.service;

import com.example.short_video.dto.response.FollowStatusResponse;
import com.example.short_video.dto.response.UserSummaryResponse;
import com.example.short_video.entity.Follow;
import com.example.short_video.entity.User;
import com.example.short_video.exception.AppException;
import com.example.short_video.repository.FollowRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.service.imp.FollowServiceImpl;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private FollowServiceImpl followService;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).username("user1").email("u1@test.com").build();
        user2 = User.builder().id(2L).username("user2").email("u2@test.com").build();
        user3 = User.builder().id(3L).username("user3").email("u3@test.com").build();
    }

    @Test
    @DisplayName("toggleFollow: Follow user thành công khi chưa theo dõi")
    void toggleFollow_followSuccess() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(followRepository.findByFollowerAndFollowing(user1, user2)).thenReturn(Optional.empty());
        when(followRepository.existsByFollowerAndFollowing(user2, user1)).thenReturn(false);
        when(followRepository.countByFollowing(user2)).thenReturn(1L);
        when(followRepository.countByFollower(user2)).thenReturn(0L);

        FollowStatusResponse response = followService.toggleFollow(user1, 2L);

        assertTrue(response.isFollowing());
        assertFalse(response.isFollowedBy());
        assertFalse(response.isMutual());
        assertEquals(1L, response.getFollowerCount());
        verify(followRepository).save(any(Follow.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/2/notifications"), any(Object.class));
    }

    @Test
    @DisplayName("toggleFollow: Unfollow user thành công khi đã theo dõi trước đó")
    void toggleFollow_unfollowSuccess() {
        Follow existingFollow = Follow.builder().id(10L).follower(user1).following(user2).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(followRepository.findByFollowerAndFollowing(user1, user2)).thenReturn(Optional.of(existingFollow));
        when(followRepository.existsByFollowerAndFollowing(user2, user1)).thenReturn(false);
        when(followRepository.countByFollowing(user2)).thenReturn(0L);
        when(followRepository.countByFollower(user2)).thenReturn(0L);

        FollowStatusResponse response = followService.toggleFollow(user1, 2L);

        assertFalse(response.isFollowing());
        verify(followRepository).delete(existingFollow);
    }

    @Test
    @DisplayName("toggleFollow: Ném ngoại lệ khi tự follow chính mình")
    void toggleFollow_selfFollow_throwsException() {
        AppException ex = assertThrows(AppException.class, () -> followService.toggleFollow(user1, 1L));
        assertEquals("Bạn không thể theo dõi chính mình", ex.getMessage());
    }

    @Test
    @DisplayName("getFollowStatus: Trả về đầy đủ trạng thái follow và bạn bè tương hỗ")
    void getFollowStatus_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(followRepository.existsByFollowerAndFollowing(user1, user2)).thenReturn(true);
        when(followRepository.existsByFollowerAndFollowing(user2, user1)).thenReturn(true);
        when(followRepository.countByFollowing(user2)).thenReturn(5L);
        when(followRepository.countByFollower(user2)).thenReturn(3L);

        FollowStatusResponse response = followService.getFollowStatus(user1, 2L);

        assertTrue(response.isFollowing());
        assertTrue(response.isFollowedBy());
        assertTrue(response.isMutual());
        assertEquals(5L, response.getFollowerCount());
        assertEquals(3L, response.getFollowingCount());
    }

    @Test
    @DisplayName("getFollowers: Lấy danh sách followers có phân trang và gắn cờ quan hệ chính xác")
    void getFollowers_paginated() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        Page<User> mockPage = new PageImpl<>(List.of(user3), PageRequest.of(0, 10), 1);
        when(followRepository.findFollowersByUser(eq(user2), any())).thenReturn(mockPage);

        when(followRepository.findFollowingIdsIn(eq(user1), anySet())).thenReturn(Set.of(3L));
        when(followRepository.findFollowerIdsIn(eq(user1), anySet())).thenReturn(Collections.emptySet());

        Page<UserSummaryResponse> result = followService.getFollowers(2L, user1, 0, 10);

        assertEquals(1, result.getTotalElements());
        UserSummaryResponse summary = result.getContent().get(0);
        assertEquals(3L, summary.getId());
        assertTrue(summary.isFollowing());
        assertFalse(summary.isFollowedBy());
        assertFalse(summary.isMutual());
    }

    @Test
    @DisplayName("getMutualFriends: Lấy danh sách bạn bè cùng theo dõi")
    void getMutualFriends_list() {
        when(followRepository.findMutualFriends(user1)).thenReturn(List.of(user2));

        List<UserSummaryResponse> friends = followService.getMutualFriends(user1);

        assertEquals(1, friends.size());
        assertEquals(2L, friends.get(0).getId());
        assertTrue(friends.get(0).isMutual());
        assertTrue(friends.get(0).isFollowing());
    }
}
