package com.bootsignal.domain.crawled_review.repository;

import com.bootsignal.domain.crawled_review.entity.CrawledReview;
import com.bootsignal.domain.crawled_review.entity.CrawledReviewSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface CrawledReviewRepository extends JpaRepository<CrawledReview, Long> {

    boolean existsByCourseIdAndExternalReviewId(Long courseId, String externalReviewId);

    Page<CrawledReview> findAllByCourseIdAndSource(Long courseId, CrawledReviewSource source, Pageable pageable);

    Page<CrawledReview> findAllByCourseId(Long courseId, Pageable pageable);

    @Query("""
        SELECT cr.course.id, COUNT(cr), SUM(cr.rating)
        FROM CrawledReview cr
        WHERE cr.course.id IN :courseIds AND cr.rating IS NOT NULL
        GROUP BY cr.course.id
        """)
    List<Object[]> findCrawledReviewSumsByCourseIds(@Param("courseIds") List<Long> courseIds);

    /**
     * 특정 과정의 기존 externalReviewId를 한 번에 조회한다.
     * reviewCrawlJob에서 N+1 쿼리(existsBy × N) 대신 벌크 조회로 사용한다.
     */
    @Query("SELECT cr.externalReviewId FROM CrawledReview cr WHERE cr.course.id = :courseId")
    Set<String> findExternalReviewIdsByCourseId(@Param("courseId") Long courseId);
           
    @Query("SELECT COUNT(cr) FROM CrawledReview cr WHERE cr.course.id = :courseId")
    long countReviewsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT MAX(cr.crawledAt) FROM CrawledReview cr WHERE cr.course.id = :courseId")
    Optional<Instant> findMaxCrawledAtByCourseId(@Param("courseId") Long courseId);
}

