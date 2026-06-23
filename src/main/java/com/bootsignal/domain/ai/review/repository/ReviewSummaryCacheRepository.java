package com.bootsignal.domain.ai.review.repository;

import com.bootsignal.domain.ai.review.entity.ReviewSummaryCache;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSummaryCacheRepository extends JpaRepository<ReviewSummaryCache, Long> {

	Optional<ReviewSummaryCache> findByCourseId(Long courseId);
}
