package com.bootsignal.domain.crawled_review.repository;

import com.bootsignal.domain.crawled_review.entity.CrawledReview;
import com.bootsignal.domain.crawled_review.entity.CrawledReviewSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawledReviewRepository extends JpaRepository<CrawledReview, Long> {

    boolean existsByCourseIdAndExternalReviewId(Long courseId, String externalReviewId);

    Page<CrawledReview> findAllByCourseIdAndSource(Long courseId, CrawledReviewSource source, Pageable pageable);

    Page<CrawledReview> findAllByCourseId(Long courseId, Pageable pageable);
}
