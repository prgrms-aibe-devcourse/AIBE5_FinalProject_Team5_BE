package com.bootsignal.domain.ai.review.controller;

import com.bootsignal.domain.ai.review.dto.ReviewSummaryCreateRequest;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryResponse;
import com.bootsignal.domain.ai.review.service.ReviewSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/review-summaries")
@RequiredArgsConstructor
public class ReviewSummaryController {

	// 고용24에서 크롤링된 수강후기 요약 요청을 서비스 계층으로 위임한다.
	private final ReviewSummaryService reviewSummaryService;

	@PostMapping
	public ReviewSummaryResponse createSummary(@RequestBody @Valid ReviewSummaryCreateRequest request) {
		return reviewSummaryService.createSummary(request);
	}
}
