package com.example.short_video.repository;

import com.example.short_video.entity.Follow;
import com.example.short_video.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    long countByFollowing(User following);
    long countByFollower(User follower);

    Optional<Follow> findByFollowerAndFollowing(User follower, User following);
    boolean existsByFollowerAndFollowing(User follower, User following);
    void deleteByFollowerAndFollowing(User follower, User following);

    @Query("SELECT COUNT(f1) > 0 FROM Follow f1 WHERE f1.follower = :u1 AND f1.following = :u2 " +
           "AND EXISTS (SELECT f2 FROM Follow f2 WHERE f2.follower = :u2 AND f2.following = :u1)")
    boolean areMutualFriends(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT f1.following FROM Follow f1 WHERE f1.follower = :user " +
           "AND EXISTS (SELECT f2 FROM Follow f2 WHERE f2.follower = f1.following AND f2.following = :user)")
    List<User> findMutualFriends(@Param("user") User user);
}
