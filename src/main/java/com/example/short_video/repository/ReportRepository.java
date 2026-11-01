package com.example.short_video.repository;

import com.example.short_video.Enum.ReportReason;
import com.example.short_video.Enum.ReportStatus;
import com.example.short_video.Enum.ReportTargetType;
import com.example.short_video.entity.Report;
import com.example.short_video.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<Report> findByTargetTypeOrderByCreatedAtDesc(ReportTargetType targetType, Pageable pageable);

    Page<Report> findByStatusAndTargetTypeOrderByCreatedAtDesc(ReportStatus status, ReportTargetType targetType, Pageable pageable);

    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);

    long countByTargetType(ReportTargetType targetType);

    boolean existsByReporterAndTargetTypeAndTargetId(User reporter, ReportTargetType targetType, Long targetId);

    @Query("SELECT r.targetId, COUNT(r) as cnt FROM Report r WHERE r.targetType = :targetType GROUP BY r.targetId ORDER BY cnt DESC")
    List<Object[]> findTopReportedTargets(@Param("targetType") ReportTargetType targetType, Pageable pageable);
}
