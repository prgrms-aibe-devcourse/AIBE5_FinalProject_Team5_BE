package com.bootsignal.domain.ai.harness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.agent.AiAgent;
import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.domain.ai.exception.AiRetryableException;
import com.bootsignal.domain.ai.guardrail.GuardrailResult;
import com.bootsignal.domain.ai.log.AgentExecutionLogService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentHarnessTest {

	@Mock
	private AgentExecutionLogService logService;

	@Test
	void executeRunsMatchedAgentAndLogsSuccess() {
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.PORTFOLIO_DRAFT,
			1L,
			"포트폴리오 초안 생성 요청",
			Map.of("job", "backend")
		);
		AgentHarness harness = new AgentHarness(List.of(new MockAgent(
			AgentType.PORTFOLIO_DRAFT,
			agentContext -> AgentExecutionResult.success(
				agentContext,
				"포트폴리오 초안 생성 완료",
				Map.of("draft", "백엔드 개발자 포트폴리오 초안")
			)
		)), logService);

		AgentExecutionResult result = harness.execute(context);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.output()).containsEntry("draft", "백엔드 개발자 포트폴리오 초안");
		verify(logService).start(context);
		verify(logService).completeSuccess(context.executionId(), "포트폴리오 초안 생성 완료");
	}

	@Test
	void executeThrowsWhenAgentTypeIsNotRegistered() {
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"리뷰 요약 요청"
		);
		AgentHarness harness = new AgentHarness(List.of(), logService);

		assertThatThrownBy(() -> harness.execute(context))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.AI_AGENT_NOT_FOUND);
	}

	@Test
	void executeLogsFailureWhenAgentThrowsException() {
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"리뷰 요약 요청"
		);
		AgentHarness harness = new AgentHarness(List.of(new MockAgent(
			AgentType.REVIEW_SUMMARY,
			agentContext -> {
				throw new IllegalStateException("mock openai failure");
			}
		)), logService);

		assertThatThrownBy(() -> harness.execute(context))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.AI_EXECUTION_FAILED);

		verify(logService).start(context);
		verify(logService).completeFailure(eq(context.executionId()), contains("mock openai failure"));
	}

	@Test
	void executeLogsFailureWhenAgentReturnsFailureResult() {
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"리뷰 요약 요청"
		);
		AgentHarness harness = new AgentHarness(List.of(new MockAgent(
			AgentType.REVIEW_SUMMARY,
			agentContext -> AgentExecutionResult.failure(agentContext, "리뷰 데이터가 부족합니다.")
		)), logService);

		AgentExecutionResult result = harness.execute(context);

		assertThat(result.isSuccess()).isFalse();
		verify(logService).start(context);
		verify(logService).completeFailure(context.executionId(), result.errorMessage());
	}

	@Test
	void executeStopsBeforeAgentWhenInputGuardrailFails() {
		AtomicInteger executeCount = new AtomicInteger();
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.PORTFOLIO_DRAFT,
			1L,
			"포트폴리오 초안 생성 요청"
		);
		AgentHarness harness = new AgentHarness(
			List.of(new MockAgent(
				AgentType.PORTFOLIO_DRAFT,
				agentContext -> {
					executeCount.incrementAndGet();
					return AgentExecutionResult.success(agentContext, "실행 완료", Map.of());
				}
			)),
			logService,
			RetryPolicy.noRetry(),
			List.of(agentContext -> GuardrailResult.fail(ErrorCode.AI_INPUT_INVALID, "필수 입력이 없습니다.")),
			List.of()
		);

		assertThatThrownBy(() -> harness.execute(context))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.AI_INPUT_INVALID);

		assertThat(executeCount).hasValue(0);
		verify(logService).start(context);
		verify(logService).completeFailure(context.executionId(), "필수 입력이 없습니다.");
	}

	@Test
	void executeRetriesRetryableExceptionAndThenLogsSuccess() {
		AtomicInteger executeCount = new AtomicInteger();
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"리뷰 요약 요청"
		);
		AgentHarness harness = new AgentHarness(
			List.of(new MockAgent(
				AgentType.REVIEW_SUMMARY,
				agentContext -> {
					if (executeCount.incrementAndGet() == 1) {
						throw new AiRetryableException(ErrorCode.AI_EXECUTION_FAILED, "일시적인 OpenAI 오류입니다.");
					}
					return AgentExecutionResult.success(agentContext, "리뷰 요약 완료", Map.of("summary", "좋은 리뷰"));
				}
			)),
			logService,
			RetryPolicy.of(2, 0),
			List.of(),
			List.of()
		);

		AgentExecutionResult result = harness.execute(context);

		assertThat(result.isSuccess()).isTrue();
		assertThat(executeCount).hasValue(2);
		verify(logService).recordRetry(context.executionId(), "일시적인 OpenAI 오류입니다.");
		verify(logService).completeSuccess(context.executionId(), "리뷰 요약 완료");
	}

	@Test
	void executeDoesNotRetryNonRetryableException() {
		AtomicInteger executeCount = new AtomicInteger();
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"리뷰 요약 요청"
		);
		AgentHarness harness = new AgentHarness(
			List.of(new MockAgent(
				AgentType.REVIEW_SUMMARY,
				agentContext -> {
					executeCount.incrementAndGet();
					throw new AiNonRetryableException(ErrorCode.AI_INPUT_INVALID, "리뷰 데이터가 없습니다.");
				}
			)),
			logService,
			RetryPolicy.of(3, 0),
			List.of(),
			List.of()
		);

		assertThatThrownBy(() -> harness.execute(context))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.AI_INPUT_INVALID);

		assertThat(executeCount).hasValue(1);
		verify(logService, never()).recordRetry(eq(context.executionId()), contains("리뷰 데이터가 없습니다."));
		verify(logService).completeFailure(context.executionId(), "리뷰 데이터가 없습니다.");
	}

	@Test
	void executeRetriesRetryableOutputGuardrailFailure() {
		AtomicInteger executeCount = new AtomicInteger();
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.PORTFOLIO_DRAFT,
			1L,
			"포트폴리오 초안 생성 요청"
		);
		AgentHarness harness = new AgentHarness(
			List.of(new MockAgent(
				AgentType.PORTFOLIO_DRAFT,
				agentContext -> {
					if (executeCount.incrementAndGet() == 1) {
						return AgentExecutionResult.success(agentContext, "", Map.of("draft", "내용만 있음"));
					}
					return AgentExecutionResult.success(agentContext, "포트폴리오 초안 생성 완료", Map.of("draft", "초안"));
				}
			)),
			logService,
			RetryPolicy.of(2, 0),
			List.of(),
			List.of((agentContext, result) -> result.outputSummary().isBlank()
				? GuardrailResult.retryableFail(ErrorCode.AI_OUTPUT_INVALID, "출력 요약이 없습니다.")
				: GuardrailResult.pass())
		);

		AgentExecutionResult result = harness.execute(context);

		assertThat(result.isSuccess()).isTrue();
		assertThat(executeCount).hasValue(2);
		verify(logService).recordRetry(context.executionId(), "출력 요약이 없습니다.");
		verify(logService).completeSuccess(context.executionId(), "포트폴리오 초안 생성 완료");
	}

	@Test
	void executeThrowsRetryExhaustedWhenRetryableFailureContinues() {
		AtomicInteger executeCount = new AtomicInteger();
		AgentExecutionContext context = AgentExecutionContext.of(
			AgentType.REVIEW_SUMMARY,
			1L,
			"리뷰 요약 요청"
		);
		AgentHarness harness = new AgentHarness(
			List.of(new MockAgent(
				AgentType.REVIEW_SUMMARY,
				agentContext -> {
					executeCount.incrementAndGet();
					throw new AiRetryableException(ErrorCode.AI_EXECUTION_FAILED, "계속 실패합니다.");
				}
			)),
			logService,
			RetryPolicy.of(2, 0),
			List.of(),
			List.of()
		);

		assertThatThrownBy(() -> harness.execute(context))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.AI_RETRY_EXHAUSTED);

		assertThat(executeCount).hasValue(2);
		verify(logService, times(1)).recordRetry(context.executionId(), "계속 실패합니다.");
		verify(logService).completeFailure(context.executionId(), "AI Agent 재시도 횟수를 초과했습니다.");
	}

	private record MockAgent(
		AgentType type,
		Function<AgentExecutionContext, AgentExecutionResult> executor
	) implements AiAgent {

		@Override
		public AgentExecutionResult execute(AgentExecutionContext context) {
			return executor.apply(context);
		}
	}
}
