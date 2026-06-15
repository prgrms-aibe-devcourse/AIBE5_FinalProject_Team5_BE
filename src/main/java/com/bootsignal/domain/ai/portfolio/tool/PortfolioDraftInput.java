package com.bootsignal.domain.ai.portfolio.tool;

import java.util.List;

// Agent 내부 tool들이 공유하는 정규화된 포트폴리오 입력 모델이다.
public record PortfolioDraftInput(
	String targetJob,
	List<String> skills,
	List<PortfolioProjectInput> projects,
	String education,
	String careerSummary,
	String toneDescription,
	String userNickname
) {
	public PortfolioDraftInput {
		skills = skills == null ? List.of() : List.copyOf(skills);
		projects = projects == null ? List.of() : List.copyOf(projects);
	}
}
