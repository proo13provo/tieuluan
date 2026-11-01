package com.example.short_video.repository;

import com.example.short_video.entity.Follow;
import com.example.short_video.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    long countByFollowing(User following);
    long countByFollower(User follower);

    Optional<Follow> findByFollowerAndFollowing(User follower, User following);
    boolean existsByFollowerAndFollowing(User follower, User following);
    void deleteByFollowerAndFollowing(User follower, User following);

    @Query(value = "SELECT f.follower FROM Follow f WHERE f.following = :user ORDER BY f.createdAt DESC",
           countQuery = "SELECT count(f) FROM Follow f WHERE f.following = :user")
    Page<User> findFollowersByUser(@Param("user") User user, Pageable pageable);

    @Query(value = "SELECT f.following FROM Follow f WHERE f.follower = :user ORDER BY f.createdAt DESC",
           countQuery = "SELECT count(f) FROM Follow f WHERE f.follower = :user")
    Page<User> findFollowingByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(f1) > 0 FROM Follow f1 WHERE f1.follower = :u1 AND f1.following = :u2 " +
           "AND EXISTS (SELECT f2 FROM Follow f2 WHERE f2.follower = :u2 AND f2.following = :u1)")
    boolean areMutualFriends(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT f1.following FROM Follow f1 WHERE f1.follower = :user " +
           "AND EXISTS (SELECT f2 FROM Follow f2 WHERE f2.follower = f1.following AND f2.following = :user)")
    List<User> findMutualFriends(@Param("user") User user);

    @Query(value = "SELECT f1.following FROM Follow f1 WHERE f1.follower = :user " +
           "AND EXISTS (SELECT f2 FROM Follow f2 WHERE f2.follower = f1.following AND f2.following = :user)",
           countQuery = "SELECT count(f1) FROM Follow f1 WHERE f1.follower = :user " +
           "AND EXISTS (SELECT f2 FROM Follow f2 WHERE f2.follower = f1.following AND f2.following = :user)")
    Page<User> findMutualFriends(@Param("user") User user, Pageable pageable);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower = :user AND f.following.id IN :targetUserIds")
    Set<Long> findFollowingIdsIn(@Param("user") User user, @Param("targetUserIds") Collection<Long> targetUserIds);

    @Query("SELECT f.follower.id FROM Follow f WHERE f.following = :user AND f.follower.id IN :targetUserIds")
    Set<Long> findFollowerIdsIn(@Param("user") User user, @Param("targetUserIds") Collection<Long> targetUserIds);

    @Query("SELECT f.following.id, COUNT(f) FROM Follow f WHERE f.following.id IN :userIds GROUP BY f.following.id")
    List<Object[]> countFollowersGroupedByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("SELECT f.follower.id, COUNT(f) FROM Follow f WHERE f.follower.id IN :userIds GROUP BY f.follower.id")
    List<Object[]> countFollowingGroupedByUserIds(@Param("userIds") Collection<Long> userIds);
}
