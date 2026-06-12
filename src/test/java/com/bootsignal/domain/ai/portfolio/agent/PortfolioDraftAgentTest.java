package com.bootsignal.domain.ai.portfolio.agent;

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
import com.bootsignal.domain.ai.log.AgentExecutionMetadata;
import com.bootsignal.domain.ai.log.AgentExecutionLogService;
import com.bootsignal.domain.ai.openai.OpenAiClient;
import com.bootsignal.domain.ai.openai.OpenAiRequest;
import com.bootsignal.domain.ai.openai.OpenAiResponse;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftTone;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioProjectExperienceRequest;
import com.bootsignal.domain.ai.portfolio.tool.PortfolioDraftParseTool;
import com.bootsignal.domain.ai.portfolio.tool.PortfolioInputNormalizeTool;
import com.bootsignal.domain.ai.portfolio.tool.PortfolioPromptRenderTool;
import com.bootsignal.domain.ai.prompt.PromptTemplateRegistry;
import com.bootsignal.global.config.AiPromptConfig;
import com.bootsignal.global.config.properties.OpenAiProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioDraftAgentTest {

	// OpenAI 호출은 Mock으로 두고 Agent의 프롬프트 생성, 파싱, 재시도 흐름만 검증한다.
	@Mock
	private OpenAiClient openAiClient;

	@Mock
	private AgentExecutionLogService logService;

	@Test
	void executeCallsOpenAiAndReturnsParsedDraft() {
		PortfolioDraftAgent agent = agent();
		AgentExecutionContext context = context();
		when(openAiClient.complete(any())).thenReturn(response(validJson()));

		AgentExecutionResult result = agent.execute(context);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.metadata().promptVersion()).isEqualTo("PORTFOLIO_DRAFT:v2");
		assertThat(result.metadata().model()).isEqualTo("gpt-4o");
		assertThat(result.output().get("draft")).isInstanceOf(PortfolioDraftContent.class);
		PortfolioDraftContent draft = (PortfolioDraftContent) result.output().get("draft");
		assertThat(draft.introduction()).contains("백엔드 개발자");
		assertThat(draft.projectDescriptions()).hasSize(1);

		ArgumentCaptor<OpenAiRequest> captor = ArgumentCaptor.forClass(OpenAiRequest.class);
		verify(openAiClient).complete(captor.capture());
		OpenAiRequest request = captor.getValue();
		assertThat(request.promptVersion()).isEqualTo("PORTFOLIO_DRAFT:v2");
		assertThat(request.temperature()).isEqualTo(0.2);
		assertThat(request.userPrompt()).contains("출력 JSON 스키마");
	}

	@Test
	void harnessRetriesWhenJsonParsingFailsAndThenSucceeds() {
		PortfolioDraftAgent agent = agent();
		AgentExecutionContext context = context();
		AgentHarness harness = harness(agent, RetryPolicy.of(2, 0));
		when(openAiClient.complete(any()))
			.thenReturn(response("not json"))
			.thenReturn(response(validJson()));

		AgentExecutionResult result = harness.execute(context);

		assertThat(result.isSuccess()).isTrue();
		verify(openAiClient, times(2)).complete(any());
		verify(logService).recordRetry(context.executionId(), "포트폴리오 초안 JSON 파싱에 실패했습니다.");
		verify(logService).completeSuccess(
			context.executionId(),
			"백엔드 개발자 포트폴리오 초안 생성 완료",
			new AgentExecutionMetadata("gpt-4o", "PORTFOLIO_DRAFT:v2", 10, 20, 30, null, 0.2)
		);
	}

	@Test
	void harnessThrowsRetryExhaustedWhenJsonParsingKeepsFailing() {
		PortfolioDraftAgent agent = agent();
		AgentExecutionContext context = context();
		AgentHarness harness = harness(agent, RetryPolicy.of(2, 0));
		when(openAiClient.complete(any())).thenReturn(response("not json"));

		assertThatThrownBy(() -> harness.execute(context))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.AI_RETRY_EXHAUSTED);

		verify(openAiClient, times(2)).complete(any());
		verify(logService).recordRetry(context.executionId(), "포트폴리오 초안 JSON 파싱에 실패했습니다.");
		verify(logService).completeFailure(
			context.executionId(),
			ErrorCode.AI_RETRY_EXHAUSTED,
			"AI Agent 재시도 횟수를 초과했습니다."
		);
	}

	private PortfolioDraftAgent agent() {
		PortfolioInputNormalizeTool normalizeTool = new PortfolioInputNormalizeTool();
		PortfolioPromptRenderTool promptRenderTool = new PortfolioPromptRenderTool(
			new PromptTemplateRegistry(List.of(new AiPromptConfig().portfolioDraftPromptTemplateV2())),
			normalizeTool
		);
		return new PortfolioDraftAgent(
			normalizeTool,
			promptRenderTool,
			new PortfolioDraftParseTool(new ObjectMapper()),
			openAiClient,
			new OpenAiProperties("test-key", "gpt-4o", 30_000, null, null)
		);
	}

	private AgentHarness harness(AiAgent agent, RetryPolicy retryPolicy) {
		return new AgentHarness(List.of(agent), logService, retryPolicy, List.of(), List.of());
	}

	private AgentExecutionContext context() {
		return AgentExecutionContext.of(
			AgentType.PORTFOLIO_DRAFT,
			1L,
			"포트폴리오 초안 생성 요청",
			Map.of(
				"targetJob", "백엔드 개발자",
				"skills", List.of("Java", "Spring Boot"),
				"projects", List.of(new PortfolioProjectExperienceRequest(
					"BootSignal",
					"백엔드 개발",
					"교육 과정 탐색 서비스를 구현했습니다.",
					List.of("Java", "Spring Boot"),
					"검색 API를 구현했습니다.",
					null
				)),
				"tone", PortfolioDraftTone.PROFESSIONAL,
				"toneDescription", PortfolioDraftTone.PROFESSIONAL.description()
			)
		);
	}

	private OpenAiResponse response(String content) {
		return new OpenAiResponse("gpt-4o", content, 10, 20, 30, null);
	}

	private String validJson() {
		return """
			{
			  "introduction": "백엔드 개발자를 목표로 교육 과정 탐색 서비스를 구현한 지원자입니다.",
			  "coreCompetencies": ["Spring 기반 API 구현", "검색 기능 설계"],
			  "projectDescriptions": [
			    {
			      "name": "BootSignal",
			      "summary": "교육 과정 탐색 서비스를 구현했습니다.",
			      "role": "백엔드 개발",
			      "techStack": ["Java", "Spring Boot"],
			      "highlights": ["검색 API를 구현했습니다."]
			    }
			  ],
			  "techStackSummary": "Java와 Spring Boot를 활용해 백엔드 API를 구현할 수 있습니다.",
			  "improvementSuggestions": ["성과 지표를 추가하면 좋습니다."]
			}
			""";
	}
}
