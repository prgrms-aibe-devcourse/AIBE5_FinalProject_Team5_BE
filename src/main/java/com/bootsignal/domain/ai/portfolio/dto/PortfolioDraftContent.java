package com.bootsignal.domain.ai.portfolio.dto;

import java.util.List;

// AI가 생성한 포트폴리오 초안 본문을 화면에서 바로 쓰기 쉬운 구조로 담는다.
public record PortfolioDraftContent(
	String introduction,
	List<String> coreCompetencies,
	List<PortfolioDraftProject> projectDescriptions,
	String techStackSummary,
	List<String> improvementSuggestions
) {
	public PortfolioDraftContent {
		coreCompetencies = coreCompetencies == null ? List.of() : List.copyOf(coreCompetencies);
		projectDescriptions = projectDescriptions == null ? List.of() : List.copyOf(projectDescriptions);
		improvementSuggestions = improvementSuggestions == null ? List.of() : List.copyOf(improvementSuggestions);
	}
}
