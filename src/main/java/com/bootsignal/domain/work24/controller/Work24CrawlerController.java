package com.bootsignal.domain.work24.controller;

import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewCrawlRequest;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewCrawlResponse;
import com.bootsignal.domain.work24.service.Work24CrawlerService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고용24 과정 개요 크롤링을 수동 실행하는 관리자용 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/work24/training-course-overview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class Work24CrawlerController {

	private final Work24CrawlerService work24CrawlerService;

	@PostMapping("/crawl")
	public Work24TrainingCourseOverviewCrawlResponse crawlAndSave(
		@RequestBody(required = false) Work24TrainingCourseOverviewCrawlRequest request
	) throws IOException {
		return Work24TrainingCourseOverviewCrawlResponse.from(work24CrawlerService.crawlAndSave(request));
	}
}
