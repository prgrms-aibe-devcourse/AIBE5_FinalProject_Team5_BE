package com.bootsignal.domain.ai.review.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bootsignal.domain.ai.agent.AiAgent;
import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.harness.AgentHarness;
import com.bootsignal.domain.ai.harness.RetryPolicy;
import com.bootsignal.domain.ai.log.AgentExecutionLogService;
import com.bootsignal.domain.ai.openai.OpenAiClient;
import com.bootsignal.domain.ai.openai.OpenAiRequest;
import com.bootsignal.domain.ai.openai.OpenAiResponse;
import com.bootsignal.domain.ai.prompt.PromptTemplateRegistry;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryContent;
import com.bootsignal.domain.ai.review.tool.CrawledReviewLoadTool;
import com.bootsignal.domain.ai.review.tool.CrawledReviewSnippet;
import com.bootsignal.domain.ai.review.tool.ReviewSummaryInput;
import com.bootsignal.domain.ai.review.tool.ReviewSummaryParseTool;
import com.bootsignal.domain.ai.review.tool.ReviewSummaryPromptRenderTool;
import com.bootsignal.global.config.AiPromptConfig;
import com.bootsignal.global.config.properties.OpenAiProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryAgentTest {

	// OpenAI 호출은 Mock으로 두고 Agent의 프롬프트 생성, 파싱, 재시도 흐름만 검증한다.
	@Mock
	private CrawledReviewLoadTool reviewLoadTool;

	@Mock
	private OpenAiClient openAiClient;

	@Mock
	private AgentExecutionLogService logService;

	@Test
	void executeCallsOpenAiAndReturnsParsedSummary() {
		ReviewSummaryAgent agent = agent();
		AgentExecutionContext context = context();
		when(reviewLoadTool.load(context)).thenReturn(input());
		when(openAiClient.complete(any())).thenReturn(response(validJson()));

		AgentExecutionResult result = agent.execute(context);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.metadata().promptVersion()).isEqualTo("REVIEW_SUMMARY:v2");
		assertThat(result.output()).containsEntry("courseTitle", "백엔드 과정");
		assertThat(result.output().get("summary")).isInstanceOf(ReviewSummaryContent.class);
		ReviewSummaryContent summary = (ReviewSummaryContent) result.output().get("summary");
		assertThat(summary.strengths()).contains("멘토링 만족도");

		ArgumentCaptor<OpenAiRequest> captor = ArgumentCaptor.forClass(OpenAiRequest.class);
		verify(openAiClient).complete(captor.capture());
		OpenAiRequest request = captor.getValue();
		assertThat(request.promptVersion()).isEqualTo("REVIEW_SUMMARY:v2");
		assertThat(request.userPrompt()).contains("과정명: 백엔드 과정");
		assertThat(request.userPrompt()).contains("멘토링이 좋았습니다.");
	}

	@Test
	void harnessRetriesWhenJsonParsingFailsAndThenSucceeds() {
		ReviewSummaryAgent agent = agent();
		AgentExecutionContext context = context();
		AgentHarness harness = harness(agent, RetryPolicy.of(2, 0));
		when(reviewLoadTool.load(context)).thenReturn(input());
		when(openAiClient.complete(any()))
			.thenReturn(response("not json"))
			.thenReturn(response(validJson()));

		AgentExecutionResult result = harness.execute(context);

		assertThat(result.isSuccess()).isTrue();
		verify(openAiClient, times(2)).complete(any());
		verify(logService).recordRetry(context.executionId(), "수강후기 요약 JSON 파싱에 실패했습니다.");
	}

	@Test
	void harnessThrowsRetryExhaustedWhenJsonParsingKeepsFailing() {
		ReviewSummaryAgent agent = agent();
		AgentExecutionContext context = context();
		AgentHarness harness = harness(agent, RetryPolicy.of(2, 0));
		when(reviewLoadTool.load(context)).thenReturn(input());
		when(openAiClient.complete(any())).thenReturn(response("not json"));

		assertThatThrownBy(() -> harness.execute(context))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.AI_RETRY_EXHAUSTED);

		verify(openAiClient, times(2)).complete(any());
		verify(logService).completeFailure(
			context.executionId(),
			ErrorCode.AI_RETRY_EXHAUSTED,
			"AI Agent 재시도 횟수를 초과했습니다."
		);
	}

	private ReviewSummaryAgent agent() {
		ReviewSummaryPromptRenderTool promptRenderTool = new ReviewSummaryPromptRenderTool(
			new PromptTemplateRegistry(List.of(new AiPromptConfig().reviewSummaryPromptTemplateV2())),
			reviewLoadTool
		);
		return new ReviewSummaryAgent(
			reviewLoadTool,
			promptRenderTool,
			new ReviewSummaryParseTool(new ObjectMapper()),
			openAiClient,
			new OpenAiProperties("test-key", "gpt-4o", 30_000, null, null)
		);
	}

	private AgentHarness harness(AiAgent agent, RetryPolicy retryPolicy) {
		return new AgentHarness(List.of(agent), logService, retryPolicy, List.of(), List.of());
	}

	private AgentExecutionContext context() {
		return AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"고용24 수강후기 요약 요청",
			Map.of("courseId", 1L, "maxReviewCount", 50)
		);
	}

	private ReviewSummaryInput input() {
		return new ReviewSummaryInput(
			1L,
			"백엔드 과정",
			2,
			BigDecimal.valueOf(4.5),
			List.of(
				new CrawledReviewSnippet(1L, 5, "멘토링이 좋았습니다."),
				new CrawledReviewSnippet(2L, 4, "프로젝트 경험이 도움이 됐습니다.")
			)
		);
	}

	private OpenAiResponse response(String content) {
		return new OpenAiResponse("gpt-4o", content, 10, 20, 30, null);
	}

	private String validJson() {
		return """
			{
			  "summary": "멘토링과 프로젝트 경험에 대한 만족도가 높습니다.",
			  "strengths": ["멘토링 만족도", "프로젝트 경험"],
			  "weaknesses": ["학습량이 많다는 부담이 있습니다."],
			  "recommendedFor": ["실무형 프로젝트를 원하는 학습자"],
			  "keywords": ["멘토링", "프로젝트", "실무"]
			}
			""";
	}
}
