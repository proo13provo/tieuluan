package com.example.short_video.repository;

import com.example.short_video.entity.Follow;
import com.example.short_video.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    /** Đếm số người theo dõi (follower) của một user */
    long countByFollowing(User following);

    /** Đếm số người mà user đang theo dõi (following) */
    long countByFollower(User follower);
}
