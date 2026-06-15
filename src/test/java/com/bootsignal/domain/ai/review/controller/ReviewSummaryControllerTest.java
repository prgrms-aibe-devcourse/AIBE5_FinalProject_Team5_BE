package com.bootsignal.domain.ai.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.ai.review.dto.ReviewSummaryContent;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryResponse;
import com.bootsignal.domain.ai.review.service.ReviewSummaryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReviewSummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReviewSummaryController 테스트")
class ReviewSummaryControllerTest {

	// Controller 슬라이스에서는 요청 검증과 공통 응답 래핑만 확인한다.
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewSummaryService reviewSummaryService;

	@Test
	@DisplayName("POST /api/ai/review-summaries - 수강후기 요약 성공")
	void createSummaryReturnsOkResponse() throws Exception {
		UUID executionId = UUID.randomUUID();
		ReviewSummaryContent content = new ReviewSummaryContent(
			"실무 중심 프로젝트와 멘토링에 대한 만족도가 높습니다.",
			List.of("멘토링 만족도", "팀 프로젝트 경험"),
			List.of("학습량이 많음"),
			List.of("팀 프로젝트로 성장하고 싶은 학습자"),
			List.of("멘토링", "프로젝트")
		);
		given(reviewSummaryService.createSummary(any()))
			.willReturn(ReviewSummaryResponse.from(
				executionId,
				1L,
				"백엔드 과정",
				12,
				BigDecimal.valueOf(4.75),
				content
			));

		mockMvc.perform(post("/api/ai/review-summaries")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "courseId": 1,
					  "maxReviewCount": 50
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.executionId").value(executionId.toString()))
			.andExpect(jsonPath("$.data.courseId").value(1))
			.andExpect(jsonPath("$.data.courseTitle").value("백엔드 과정"))
			.andExpect(jsonPath("$.data.reviewCount").value(12))
			.andExpect(jsonPath("$.data.summary").value("실무 중심 프로젝트와 멘토링에 대한 만족도가 높습니다."))
			.andExpect(jsonPath("$.data.strengths[0]").value("멘토링 만족도"))
			.andExpect(jsonPath("$.error").doesNotExist());

		verify(reviewSummaryService).createSummary(any());
	}

	@Test
	@DisplayName("POST /api/ai/review-summaries - 필수 입력 누락 시 400")
	void createSummaryReturnsValidationErrorWhenCourseIdIsMissing() throws Exception {
		mockMvc.perform(post("/api/ai/review-summaries")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "maxReviewCount": 50
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		verify(reviewSummaryService, never()).createSummary(any());
	}
}
