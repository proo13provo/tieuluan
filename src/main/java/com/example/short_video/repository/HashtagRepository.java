package com.example.short_video.repository;

import com.example.short_video.entity.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByName(String name);
    List<Hashtag> findByNameStartingWithIgnoreCaseOrderByUsageCountDesc(String prefix, Pageable pageable);
    List<Hashtag> findByNameContainingIgnoreCaseOrderByUsageCountDesc(String keyword, Pageable pageable);
    List<Hashtag> findAllByOrderByUsageCountDesc(Pageable pageable);
}
