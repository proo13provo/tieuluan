package com.example.short_video.repository;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    /** Lấy tất cả video của user, sắp xếp theo thời gian mới nhất */
    List<Video> findByUserOrderByCreatedAtDesc(User user);

    /** Lấy tất cả video của user theo trạng thái, sắp xếp theo thời gian mới nhất */
    List<Video> findByUserAndStatusOrderByCreatedAtDesc(User user, VideoStatus status);

    List<Video> findByid(Long id);
}
