package com.example.short_video.repository;

import com.example.short_video.entity.Follow;
import com.example.short_video.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    long countByFollowing(User following);
    long countByFollower(User follower);
}
