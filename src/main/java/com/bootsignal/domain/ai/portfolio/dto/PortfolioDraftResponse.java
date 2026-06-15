package com.bootsignal.domain.ai.portfolio.dto;

import java.util.List;
import java.util.UUID;

// API 응답에는 실행 ID와 구조화된 초안 결과만 노출한다.
public record PortfolioDraftResponse(
	UUID executionId,
	String introduction,
	List<String> coreCompetencies,
	List<PortfolioDraftProject> projectDescriptions,
	String techStackSummary,
	List<String> improvementSuggestions
) {
	public static PortfolioDraftResponse from(UUID executionId, PortfolioDraftContent content) {
		return new PortfolioDraftResponse(
			executionId,
			content.introduction(),
			content.coreCompetencies(),
			content.projectDescriptions(),
			content.techStackSummary(),
			content.improvementSuggestions()
		);
	}
}
