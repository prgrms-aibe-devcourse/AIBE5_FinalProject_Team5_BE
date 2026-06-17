package com.bootsignal.domain.crawled_review.repository;

import com.bootsignal.domain.crawled_review.entity.CrawledReview;
import com.bootsignal.domain.crawled_review.entity.CrawledReviewSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}

