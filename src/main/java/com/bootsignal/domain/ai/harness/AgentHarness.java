package com.bootsignal.domain.ai.harness;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.agent.AiAgent;
import com.bootsignal.domain.ai.guardrail.GuardrailResult;
import com.bootsignal.domain.ai.guardrail.InputGuardrail;
import com.bootsignal.domain.ai.guardrail.OutputGuardrail;
import com.bootsignal.domain.ai.log.AgentExecutionLogService;
import com.bootsignal.domain.ai.log.AgentExecutionStatus;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentHarness {

	private final Map<AgentType, AiAgent> agents;
	private final AgentExecutionLogService logService;
	private final RetryPolicy retryPolicy;
	private final List<InputGuardrail> inputGuardrails;
	private final List<OutputGuardrail> outputGuardrails;

	public AgentHarness(List<AiAgent> agents, AgentExecutionLogService logService) {
		// 단위 테스트에서는 Guardrail과 재시도 정책 없이도 Harness를 검증할 수 있게 둔다.
		this(agents, logService, RetryPolicy.noRetry(), List.of(), List.of());
	}

	@Autowired
	public AgentHarness(
		List<AiAgent> agents,
		AgentExecutionLogService logService,
		RetryPolicy retryPolicy,
		List<InputGuardrail> inputGuardrails,
		List<OutputGuardrail> outputGuardrails
	) {
		this.agents = agents.stream()
			.collect(Collectors.toUnmodifiableMap(AiAgent::type, Function.identity()));
		this.logService = logService;
		this.retryPolicy = retryPolicy;
		this.inputGuardrails = List.copyOf(inputGuardrails);
		this.outputGuardrails = List.copyOf(outputGuardrails);
	}

	public AgentExecutionResult execute(AgentExecutionContext context) {
		// Harness는 검증, 재시도, 로그 저장을 담당하고 실제 작업은 Agent에 위임한다.
		AiAgent agent = findAgent(context.agentType());
		logService.start(context);

		try {
			validateInput(context);
			AgentExecutionResult result = executeWithRetry(agent, context);

			if (result.status() == AgentExecutionStatus.SUCCESS) {
				logService.completeSuccess(context.executionId(), result.outputSummary());
			} else {
				logService.completeFailure(context.executionId(), result.errorMessage());
			}
			return result;
		} catch (BootSignalException exception) {
			logService.completeFailure(context.executionId(), exception.getMessage());
			throw exception;
		} catch (RuntimeException exception) {
			logService.completeFailure(context.executionId(), exception.getMessage());
			throw new BootSignalException(ErrorCode.AI_EXECUTION_FAILED, exception.getMessage());
		}
	}

	private AgentExecutionResult executeWithRetry(AiAgent agent, AgentExecutionContext context) {
		// 재시도 가능 예외만 다시 실행하고, 입력 오류처럼 확정 실패인 경우는 즉시 종료한다.
		int attempt = 1;
		while (true) {
			try {
				return executeOnce(agent, context);
			} catch (RuntimeException exception) {
				if (retryPolicy.canRetry(exception, attempt)) {
					logService.recordRetry(context.executionId(), exception.getMessage());
					retryPolicy.waitBeforeRetry();
					attempt++;
					continue;
				}
				if (retryPolicy.isRetryableException(exception)) {
					throw retryPolicy.exhausted(exception);
				}
				throw exception;
			}
		}
	}

	private AgentExecutionResult executeOnce(AiAgent agent, AgentExecutionContext context) {
		AgentExecutionResult result = agent.execute(context);
		if (result == null) {
			throw new IllegalStateException("AI Agent 실행 결과가 없습니다.");
		}
		if (result.status() == AgentExecutionStatus.SUCCESS) {
			validateOutput(context, result);
		}
		return result;
	}

	private void validateInput(AgentExecutionContext context) {
		// 입력 Guardrail은 Agent 실행 전 데이터 부족이나 권한 문제를 빠르게 차단한다.
		for (InputGuardrail guardrail : inputGuardrails) {
			GuardrailResult result = guardrail.validate(context);
			if (!result.valid()) {
				throw result.toException();
			}
		}
	}

	private void validateOutput(AgentExecutionContext context, AgentExecutionResult agentResult) {
		// 출력 Guardrail은 깨진 응답이나 필수 필드 누락을 저장 전에 차단한다.
		for (OutputGuardrail guardrail : outputGuardrails) {
			GuardrailResult result = guardrail.validate(context, agentResult);
			if (!result.valid()) {
				throw result.toException();
			}
		}
	}

	private AiAgent findAgent(AgentType agentType) {
		AiAgent agent = agents.get(agentType);
		if (agent == null) {
			throw new BootSignalException(ErrorCode.AI_AGENT_NOT_FOUND);
		}
		return agent;
	}
}
