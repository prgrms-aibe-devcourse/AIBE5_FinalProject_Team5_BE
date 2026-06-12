package com.bootsignal.domain.course.repository;

import com.bootsignal.domain.course.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseRepository extends JpaRepository<Course, Long>,
        JpaSpecificationExecutor<Course> {

    Optional<Course> findByTrprId(String trprId);

    Page<Course> findByReviewCrawledAtIsNull(Pageable pageable);

    // 목록 조회 시 institution을 한 번에 fetch하여 N+1 방지
    @EntityGraph(attributePaths = {"institution"})
    @Query("select c from Course c")
    org.springframework.data.domain.Page<Course> findAllWithInstitution(
            org.springframework.data.domain.Pageable pageable);
}
