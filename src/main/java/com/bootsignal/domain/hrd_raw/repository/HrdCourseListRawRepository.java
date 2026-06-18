package com.bootsignal.domain.hrd_raw.repository;

import com.bootsignal.domain.hrd_raw.entity.HrdCourseListRaw;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface HrdCourseListRawRepository extends JpaRepository<HrdCourseListRaw, Long> {

    Optional<HrdCourseListRaw> findByTrprIdAndTrprDegr(String trprId, Integer trprDegr);

    Page<HrdCourseListRaw> findByIsRefinedFalse(Pageable pageable);

    @Query("SELECT l FROM HrdCourseListRaw l " +
           "LEFT JOIN HrdCourseDetailRaw d ON l.trprId = d.trprId AND l.trprDegr = d.trprDegr " +
           "WHERE d.trprId IS NULL")
    Page<HrdCourseListRaw> findUncollectedDetails(Pageable pageable);

    @Query("SELECT l FROM HrdCourseListRaw l " +
           "LEFT JOIN HrdTrainingScheduleRaw s ON l.trprId = s.trprId AND l.trprDegr = s.trprDegr " +
           "WHERE s.trprId IS NULL")
    Page<HrdCourseListRaw> findUncollectedSchedules(Pageable pageable);
}

