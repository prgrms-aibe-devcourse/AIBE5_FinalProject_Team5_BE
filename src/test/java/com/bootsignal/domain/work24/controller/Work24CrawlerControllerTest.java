package com.bootsignal.domain.work24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverview;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewSaveResult;
import com.bootsignal.domain.work24.service.Work24CrawlerService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = Work24CrawlerController.class)
@AutoConfigureMockMvc(addFilters = false)
class Work24CrawlerControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Work24CrawlerService work24CrawlerService;

	@Test
	void crawlAndSaveWrapsSuccessResponse() throws Exception {
		Work24TrainingCourseOverview overview = new Work24TrainingCourseOverview(
			"https://example.com/course",
			"[훈련대상자]\n- 취업 준비생",
			"[인재상]\n- 데이터 처리 역량 확보",
			Instant.parse("2026-05-27T00:00:00Z")
		);
		given(work24CrawlerService.crawlAndSave(any()))
			.willReturn(new Work24TrainingCourseOverviewSaveResult(overview, "build/crawled/test.json"));

		mockMvc.perform(post("/api/work24/training-course-overview/crawl")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.sourceUrl").value("https://example.com/course"))
			.andExpect(jsonPath("$.data.savedPath").value("build/crawled/test.json"))
			.andExpect(jsonPath("$.data.trainingTargetRequirements").value("[훈련대상자]\n- 취업 준비생"))
			.andExpect(jsonPath("$.data.trainingGoal").value("[인재상]\n- 데이터 처리 역량 확보"))
			.andExpect(jsonPath("$.data.crawledAt").value("2026-05-27T00:00:00Z"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void crawlAndSaveReturnsBadRequestWhenServiceThrowsIllegalArgumentException() throws Exception {
		given(work24CrawlerService.crawlAndSave(any()))
			.willThrow(new IllegalArgumentException("크롤링 URL 형식이 올바르지 않습니다."));

		mockMvc.perform(post("/api/work24/training-course-overview/crawl")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"invalid-url\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
			.andExpect(jsonPath("$.error.message").value("크롤링 URL 형식이 올바르지 않습니다."))
			.andExpect(jsonPath("$.error.fieldErrors").isArray());
	}
}
