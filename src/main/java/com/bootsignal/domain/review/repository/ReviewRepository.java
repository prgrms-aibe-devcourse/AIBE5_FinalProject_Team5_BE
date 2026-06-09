package com.bootsignal.domain.review.repository;

import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.entity.ReviewType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndCourseSessionId(Long userId, Long courseSessionId);

    Optional<Review> findByIdAndDeletedAtIsNull(Long id);

    @Query(
        value = """
            SELECT r FROM Review r
            JOIN FETCH r.user u
            JOIN FETCH r.courseSession cs
            WHERE r.deletedAt IS NULL
              AND r.course.id = :courseId
              AND (:reviewType IS NULL OR r.reviewType = :reviewType)
            """,
        countQuery = """
            SELECT count(r) FROM Review r
            WHERE r.deletedAt IS NULL
              AND r.course.id = :courseId
              AND (:reviewType IS NULL OR r.reviewType = :reviewType)
            """
    )
    Page<Review> findAllByCourseId(
        @Param("courseId") Long courseId,
        @Param("reviewType") ReviewType reviewType,
        Pageable pageable
    );
}