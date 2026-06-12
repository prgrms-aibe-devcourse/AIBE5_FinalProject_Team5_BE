package com.bootsignal.domain.crawled_review.service;

import com.bootsignal.domain.crawled_review.dto.CrawledReviewResponse;
import com.bootsignal.domain.crawled_review.repository.CrawledReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawledReviewService {

    private final CrawledReviewRepository crawledReviewRepository;

    public Page<CrawledReviewResponse> getList(Long courseId, Pageable pageable) {
        return crawledReviewRepository.findAllByCourseId(courseId, pageable)
                .map(CrawledReviewResponse::from);
    }
}
