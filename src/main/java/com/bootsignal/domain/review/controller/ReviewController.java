package com.bootsignal.domain.review.controller;

import com.bootsignal.domain.review.dto.ReviewCreateRequest;
import com.bootsignal.domain.review.dto.ReviewResponse;
import com.bootsignal.domain.review.dto.ReviewStatisticsResponse;
import com.bootsignal.domain.review.dto.ReviewUpdateRequest;
import com.bootsignal.domain.review.dto.VerifiedReviewDetailRequest;
import com.bootsignal.domain.review.entity.ReviewType;
import com.bootsignal.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 과정 리뷰와 인증 리뷰 상세 설문 통계 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/courses/{courseId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
        @PathVariable Long courseId,
        @RequestBody @Valid ReviewCreateRequest request
    ) {
        return reviewService.create(courseId, request);
    }

    @GetMapping("/api/courses/{courseId}/reviews")
    public Page<ReviewResponse> getList(
        @PathVariable Long courseId,
        @RequestParam(required = false) ReviewType reviewType,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return reviewService.getList(courseId, reviewType, pageable);
    }

    @GetMapping("/api/courses/{courseId}/reviews/statistics")
    public ReviewStatisticsResponse getStatistics(@PathVariable Long courseId) {
        return reviewService.getStatistics(courseId);
    }

    @GetMapping("/api/reviews/{reviewId}")
    public ReviewResponse get(@PathVariable Long reviewId) {
        return reviewService.get(reviewId);
    }

    @PatchMapping("/api/reviews/{reviewId}")
    public ReviewResponse update(
        @PathVariable Long reviewId,
        @RequestBody @Valid ReviewUpdateRequest request
    ) {
        return reviewService.update(reviewId, request);
    }

    @PatchMapping("/api/reviews/{reviewId}/verify")
    public ReviewResponse upgrade(
        @PathVariable Long reviewId,
        @RequestBody @Valid VerifiedReviewDetailRequest request
    ) {
        return reviewService.upgrade(reviewId, request);
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long reviewId) {
        reviewService.delete(reviewId);
    }
}
