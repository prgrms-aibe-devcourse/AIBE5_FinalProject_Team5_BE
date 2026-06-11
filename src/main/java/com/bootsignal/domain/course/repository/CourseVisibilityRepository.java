package com.bootsignal.domain.course.repository;

import com.bootsignal.domain.course.entity.CourseStatus;
import com.bootsignal.domain.course.entity.CourseVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseVisibilityRepository extends JpaRepository<CourseVisibility, Long> {

    Optional<CourseVisibility> findByCourseId(Long courseId);

    boolean existsByCourseIdAndStatus(Long courseId, CourseStatus status);
}
