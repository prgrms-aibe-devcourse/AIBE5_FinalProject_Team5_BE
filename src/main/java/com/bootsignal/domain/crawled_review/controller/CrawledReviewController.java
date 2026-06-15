package com.bootsignal.domain.crawled_review.controller;

import com.bootsignal.domain.crawled_review.dto.CrawledReviewResponse;
import com.bootsignal.domain.crawled_review.service.CrawledReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CrawledReviewController {

    private final CrawledReviewService crawledReviewService;

    @GetMapping("/api/courses/{courseId}/crawled-reviews")
    public Page<CrawledReviewResponse> getList(
            @PathVariable Long courseId,
            @PageableDefault(size = 10, sort = "crawledAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return crawledReviewService.getList(courseId, pageable);
    }
}
