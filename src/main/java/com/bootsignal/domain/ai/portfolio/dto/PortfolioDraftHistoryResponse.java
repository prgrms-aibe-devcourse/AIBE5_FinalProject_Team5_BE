package com.bootsignal.domain.ai.portfolio.dto;

import com.bootsignal.domain.ai.portfolio.entity.PortfolioDraftHistory;
import java.time.LocalDateTime;

public record PortfolioDraftHistoryResponse(
	Long historyId,
	String executionId,
	String targetJob,
	String tone,
	LocalDateTime createdAt
) {
	public static PortfolioDraftHistoryResponse from(PortfolioDraftHistory history) {
		return new PortfolioDraftHistoryResponse(
			history.getId(),
			history.getExecutionId(),
			history.getTargetJob(),
			history.getTone(),
			history.getCreatedAt()
		);
	}
}
