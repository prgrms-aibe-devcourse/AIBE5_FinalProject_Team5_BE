package com.bootsignal.domain.ai.log;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	name = "ai_agent_execution_log",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_ai_agent_execution_log_execution_id", columnNames = "execution_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentExecutionLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "execution_id", nullable = false, length = 36)
	private String executionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "agent_type", nullable = false, length = 50)
	private AgentType agentType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AgentExecutionStatus status;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "input_summary", columnDefinition = "text")
	private String inputSummary;

	// 원문 입력을 보관하지 않고 동일 입력 여부만 비교하기 위한 해시를 저장한다.
	@Column(name = "input_hash", nullable = false, length = 64)
	private String inputHash;

	@Column(name = "output_summary", columnDefinition = "text")
	private String outputSummary;

	@Column(name = "error_message", columnDefinition = "text")
	private String errorMessage;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@Column(name = "elapsed_millis")
	private Long elapsedMillis;

	private AgentExecutionLog(
		String executionId,
		AgentType agentType,
		Long userId,
		String inputSummary,
		String inputHash
	) {
		this.executionId = executionId;
		this.agentType = agentType;
		this.status = AgentExecutionStatus.RUNNING;
		this.userId = userId;
		this.inputSummary = inputSummary;
		this.inputHash = inputHash;
		this.retryCount = 0;
		this.startedAt = LocalDateTime.now();
	}

	public static AgentExecutionLog start(
		String executionId,
		AgentType agentType,
		Long userId,
		String inputSummary,
		String inputHash
	) {
		return new AgentExecutionLog(executionId, agentType, userId, inputSummary, inputHash);
	}

	public void markSuccess(String outputSummary) {
		this.outputSummary = outputSummary;
		this.errorMessage = null;
		finish(AgentExecutionStatus.SUCCESS);
	}

	public void markRetrying(String errorMessage) {
		// 재시도 중에도 마지막 실패 원인을 남겨 운영자가 흐름을 추적할 수 있게 한다.
		this.status = AgentExecutionStatus.RETRYING;
		this.errorMessage = errorMessage;
		this.retryCount++;
	}

	public void markFailed(String errorMessage) {
		this.errorMessage = errorMessage;
		finish(AgentExecutionStatus.FAILED);
	}

	private void finish(AgentExecutionStatus status) {
		this.status = status;
		this.finishedAt = LocalDateTime.now();
		this.elapsedMillis = Duration.between(startedAt, finishedAt).toMillis();
	}
}
