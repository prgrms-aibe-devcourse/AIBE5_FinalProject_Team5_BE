package com.bootsignal.domain.course_session.repository;

import com.bootsignal.domain.course_session.entity.CourseSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

    List<CourseSession> findByCourse_Id(Long courseId);

    Optional<CourseSession> findByTrprIdAndTrprDegr(String trprId, Integer trprDegr);

    // 과정 회차 목록: 훈련 시작일 오름차순
    List<CourseSession> findByCourse_IdOrderByTraStartDateAsc(Long courseId);

    Optional<CourseSession> findByTitleLink(String titleLink);

    List<CourseSession> findByCourse_IdIn(List<Long> courseIds);

    Page<CourseSession> findByTitleLinkIsNotNullAndCrawledAtIsNull(Pageable pageable);

    Optional<CourseSession> findFirstByCourse_IdAndTitleLinkIsNotNull(Long courseId);
}