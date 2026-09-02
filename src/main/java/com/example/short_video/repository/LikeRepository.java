package com.example.short_video.repository;

import com.example.short_video.entity.Like;
import com.example.short_video.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * Đếm tổng số lượt thích trên tất cả video của một user.
     * Tức là tổng like mà người khác đã like các video của user này.
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.video.user = :user")
    long countTotalLikesByVideoOwner(@Param("user") User user);
}
