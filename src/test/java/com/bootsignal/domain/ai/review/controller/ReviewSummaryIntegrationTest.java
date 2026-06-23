package com.bootsignal.domain.ai.review.controller;

import static com.bootsignal.support.AuthCookieTestUtils.extractAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.ai.log.AgentExecutionLog;
import com.bootsignal.domain.ai.log.AgentExecutionLogRepository;
import com.bootsignal.domain.ai.log.AgentExecutionStatus;
import com.bootsignal.domain.ai.openai.OpenAiClient;
import com.bootsignal.domain.ai.openai.OpenAiRequest;
import com.bootsignal.domain.ai.openai.OpenAiResponse;
import com.bootsignal.domain.ai.review.repository.ReviewSummaryCacheRepository;
import com.bootsignal.domain.ai.review.tool.CrawledReviewLoadTool;
import com.bootsignal.domain.ai.review.tool.CrawledReviewSnippet;
import com.bootsignal.domain.ai.review.tool.ReviewSummaryInput;
import com.bootsignal.domain.crawled_review.repository.CrawledReviewRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리뷰 요약 AI API가 로그인 인증, 외부 AI 응답 처리, 실행 로그 저장까지 연결되는지 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewSummaryIntegrationTest {

	// 회원가입부터 JWT 인증 API 호출, Agent 실행 로그 저장까지 실제 웹 흐름으로 검증한다.
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AgentExecutionLogRepository logRepository;

	@MockitoBean
	private CrawledReviewLoadTool reviewLoadTool;

	@MockitoBean
	private OpenAiClient openAiClient;

	@MockitoBean
	private CrawledReviewRepository crawledReviewRepository;

	@MockitoBean
	private ReviewSummaryCacheRepository reviewSummaryCacheRepository;

	@Test
	void createReviewSummaryWithLoginAccessToken() throws Exception {
		given(crawledReviewRepository.findReviewSnapshotByCourseId(1L))
			.willReturn(Optional.of(new Object[]{2L, Instant.now()}));
		given(reviewSummaryCacheRepository.findByCourseId(1L)).willReturn(Optional.empty());
		given(reviewLoadTool.load(any())).willReturn(new ReviewSummaryInput(
			1L,
			"백엔드 과정",
			2,
			BigDecimal.valueOf(4.50),
			List.of(
				new CrawledReviewSnippet(1L, 5, "멘토링과 팀 프로젝트가 좋았습니다."),
				new CrawledReviewSnippet(2L, 4, "학습량은 많지만 실무 감각을 익히는 데 도움이 됐습니다.")
			)
		));
		given(openAiClient.complete(any()))
			.willReturn(new OpenAiResponse("gpt-4o", validReviewSummaryJson(), 100, 200, 300, null));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "review-summary-e2e@example.com",
					  "password": "password123",
					  "nickname": "reviewSummaryUser"
					}
					"""))
			.andExpect(status().isCreated());

		String accessToken = extractAccessToken(mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "review-summary-e2e@example.com",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andReturn());

		String summaryResponse = mockMvc.perform(post("/api/ai/review-summaries")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "courseId": 1,
					  "maxReviewCount": 50
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.courseId").value(1))
			.andExpect(jsonPath("$.data.courseTitle").value("백엔드 과정"))
			.andExpect(jsonPath("$.data.reviewCount").value(2))
			.andExpect(jsonPath("$.data.summary").value("멘토링과 프로젝트 경험에 대한 만족도가 높습니다."))
			.andExpect(jsonPath("$.data.strengths[0]").value("멘토링 만족도"))
			.andExpect(jsonPath("$.error").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		JsonNode data = objectMapper.readTree(summaryResponse).path("data");
		UUID executionId = UUID.fromString(data.path("executionId").asText());
		AgentExecutionLog log = logRepository.findByExecutionId(executionId.toString()).orElseThrow();

		assertThat(log.getStatus()).isEqualTo(AgentExecutionStatus.SUCCESS);
		assertThat(log.getPromptVersion()).isEqualTo("REVIEW_SUMMARY:v2");
		assertThat(log.getModel()).isEqualTo("gpt-4o");
		assertThat(log.getTotalTokens()).isEqualTo(300);

		ArgumentCaptor<OpenAiRequest> captor = ArgumentCaptor.forClass(OpenAiRequest.class);
		verify(openAiClient).complete(captor.capture());
		assertThat(captor.getValue().promptVersion()).isEqualTo("REVIEW_SUMMARY:v2");
		assertThat(captor.getValue().userPrompt()).contains("과정명: 백엔드 과정");
		assertThat(captor.getValue().userPrompt()).contains("멘토링과 팀 프로젝트가 좋았습니다.");
	}

	private String validReviewSummaryJson() {
		return """
			{
			  "summary": "멘토링과 프로젝트 경험에 대한 만족도가 높습니다.",
			  "strengths": ["멘토링 만족도", "팀 프로젝트 경험"],
			  "weaknesses": ["학습량이 많다는 부담이 있습니다."],
			  "recommendedFor": ["실무형 프로젝트를 원하는 학습자"],
			  "keywords": ["멘토링", "프로젝트", "실무"]
			}
			""";
	}
}
