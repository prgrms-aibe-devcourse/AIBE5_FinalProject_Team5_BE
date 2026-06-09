package com.bootsignal.domain.ai.log;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentExecutionLogService {

	private final AgentExecutionLogRepository logRepository;

	@Transactional
	public AgentExecutionLog start(AgentExecutionContext context) {
		AgentExecutionLog log = AgentExecutionLog.start(
			context.executionId().toString(),
			context.agentType(),
			context.userId(),
			context.inputSummary(),
			hashInput(context.inputSummary())
		);
		return logRepository.save(log);
	}

	@Transactional
	public void completeSuccess(UUID executionId, String outputSummary) {
		findByExecutionId(executionId).markSuccess(outputSummary);
	}

	@Transactional
	public void recordRetry(UUID executionId, String errorMessage) {
		findByExecutionId(executionId).markRetrying(errorMessage);
	}

	@Transactional
	public void completeFailure(UUID executionId, String errorMessage) {
		findByExecutionId(executionId).markFailed(errorMessage);
	}

	private AgentExecutionLog findByExecutionId(UUID executionId) {
		return logRepository.findByExecutionId(executionId.toString())
			.orElseThrow(() -> new BootSignalException(
				ErrorCode.AI_EXECUTION_FAILED,
				"AI 실행 로그를 찾을 수 없습니다."
			));
	}

	private String hashInput(String inputSummary) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(inputSummary.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new BootSignalException(ErrorCode.AI_EXECUTION_FAILED, "AI 입력 해시 생성에 실패했습니다.");
		}
	}
}
