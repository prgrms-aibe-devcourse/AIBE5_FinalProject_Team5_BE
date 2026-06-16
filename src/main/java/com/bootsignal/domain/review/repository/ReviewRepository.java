package com.bootsignal.domain.review.repository;

import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.entity.ReviewType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 리뷰 조회, 중복 작성 검증, 활성 리뷰 존재 여부를 처리하는 JPA 저장소입니다.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndCourseSessionId(Long userId, Long courseSessionId);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.user u
        JOIN FETCH r.course c
        JOIN FETCH r.courseSession cs
        LEFT JOIN FETCH r.verifiedDetail vd
        WHERE r.id = :reviewId
          AND r.deletedAt IS NULL
        """)
    Optional<Review> findActiveByIdWithDetail(@Param("reviewId") Long reviewId);

    @Query(
        value = """
            SELECT r FROM Review r
            JOIN FETCH r.user u
            JOIN FETCH r.course c
            JOIN FETCH r.courseSession cs
            LEFT JOIN FETCH r.verifiedDetail vd
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

    /**
     * 현재 사용자가 작성한 활성 리뷰를 페이지 단위로 조회합니다.
     */
    @Query(
        value = """
            SELECT r FROM Review r
            JOIN FETCH r.user u
            JOIN FETCH r.course c
            JOIN FETCH r.courseSession cs
            WHERE r.user.id = :userId
              AND r.deletedAt IS NULL
            """,
        countQuery = """
            SELECT count(r) FROM Review r
            WHERE r.user.id = :userId
              AND r.deletedAt IS NULL
            """
    )
    Page<Review> findAllByUser(
        @Param("userId") Long userId,
        Pageable pageable
    );



    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.verifiedDetail vd
        WHERE r.deletedAt IS NULL
          AND r.course.id = :courseId
          AND r.reviewType = com.bootsignal.domain.review.entity.ReviewType.VERIFIED
        """)
    List<Review> findAllVerifiedWithDetailByCourseId(@Param("courseId") Long courseId);
}
