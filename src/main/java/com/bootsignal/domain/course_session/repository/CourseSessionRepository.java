package com.bootsignal.domain.course_session.repository;

import com.bootsignal.domain.course_session.entity.CourseSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

    List<CourseSession> findByCourse_Id(Long courseId);

    Optional<CourseSession> findByTrprIdAndTrprDegr(String trprId, Integer trprDegr);
}