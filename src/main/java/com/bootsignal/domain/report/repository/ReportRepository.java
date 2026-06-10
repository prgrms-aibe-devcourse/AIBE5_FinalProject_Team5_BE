package com.bootsignal.domain.report.repository;

import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
        SELECT r FROM Report r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:targetType IS NULL OR r.targetType = :targetType)
        ORDER BY r.createdAt DESC
        """)
    Page<Report> findByFilters(
        @Param("status") ReportStatus status,
        @Param("targetType") ReportTargetType targetType,
        Pageable pageable
    );
}
