package com.bootsignal.domain.work24.controller;

import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewCrawlRequest;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewCrawlResponse;
import com.bootsignal.domain.work24.service.Work24CrawlerService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work24/training-course-overview")
@RequiredArgsConstructor
public class Work24CrawlerController {

	private final Work24CrawlerService work24CrawlerService;

	@PostMapping("/crawl")
	public Work24TrainingCourseOverviewCrawlResponse crawlAndSave(
		@RequestBody(required = false) Work24TrainingCourseOverviewCrawlRequest request
	) throws IOException {
		return Work24TrainingCourseOverviewCrawlResponse.from(work24CrawlerService.crawlAndSave(request));
	}
}
