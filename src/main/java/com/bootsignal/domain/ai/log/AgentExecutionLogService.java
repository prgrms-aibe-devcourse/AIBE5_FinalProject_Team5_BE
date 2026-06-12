package com.bootsignal.domain.ai.log;

import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentExecutionLogService {

	// AI 실행 이력은 원문 대신 요약과 해시, 실행 메타데이터만 저장한다.
	private final AgentExecutionLogRepository logRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public AgentExecutionLog start(AgentExecutionContext context) {
		AgentExecutionLog log = AgentExecutionLog.start(
			context.executionId().toString(),
			context.agentType(),
			context.userId(),
			context.inputSummary(),
			hashInput(context)
		);
		return logRepository.save(log);
	}

	@Transactional
	public void completeSuccess(UUID executionId, String outputSummary) {
		completeSuccess(executionId, outputSummary, AgentExecutionMetadata.empty());
	}

	@Transactional
	public void completeSuccess(UUID executionId, String outputSummary, AgentExecutionMetadata metadata) {
		findByExecutionId(executionId).markSuccess(outputSummary, metadata);
	}

	@Transactional
	public void recordRetry(UUID executionId, String errorMessage) {
		findByExecutionId(executionId).markRetrying(errorMessage);
	}

	@Transactional
	public void completeFailure(UUID executionId, String errorMessage) {
		completeFailure(executionId, ErrorCode.AI_EXECUTION_FAILED, errorMessage);
	}

	@Transactional
	public void completeFailure(UUID executionId, ErrorCode errorCode, String errorMessage) {
		findByExecutionId(executionId).markFailed(errorMessage, errorCode.code());
	}

	private AgentExecutionLog findByExecutionId(UUID executionId) {
		return logRepository.findByExecutionId(executionId.toString())
			.orElseThrow(() -> new BootSignalException(
				ErrorCode.AI_EXECUTION_FAILED,
				"AI 실행 로그를 찾을 수 없습니다."
			));
	}

	private String hashInput(AgentExecutionContext context) {
		try {
			// 입력 원문을 저장하지 않고, 요약과 상세 입력의 정규화 문자열만 해시에 반영한다.
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String hashSource = context.inputSummary() + "\n" + serializeInput(context.input());
			byte[] hash = digest.digest(hashSource.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new BootSignalException(ErrorCode.AI_EXECUTION_FAILED, "AI 입력 해시 생성에 실패했습니다.");
		}
	}

	private String serializeInput(Map<String, Object> input) {
		if (input == null || input.isEmpty()) {
			return "{}";
		}
		try {
			return objectMapper.writeValueAsString(input);
		} catch (JsonProcessingException exception) {
			return input.toString();
		}
	}
}
