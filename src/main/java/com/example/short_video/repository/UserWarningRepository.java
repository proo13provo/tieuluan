package com.example.short_video.repository;

import com.example.short_video.entity.User;
import com.example.short_video.entity.UserWarning;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWarningRepository extends JpaRepository<UserWarning, Long> {

    List<UserWarning> findByUserOrderByCreatedAtDesc(User user);

    long countByUser(User user);

    @Query("SELECT uw.user, COUNT(uw) as cnt FROM UserWarning uw GROUP BY uw.user ORDER BY cnt DESC")
    List<Object[]> findMostWarnedUsers(Pageable pageable);
}
