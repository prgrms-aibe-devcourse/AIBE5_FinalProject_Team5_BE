package com.bootsignal.domain.ai.portfolio.tool;

import java.util.List;

// 정규화된 프로젝트 경험은 프롬프트 생성과 출력 검증의 기준이 된다.
public record PortfolioProjectInput(
	String name,
	String role,
	String description,
	List<String> techStack,
	String achievement,
	String link
) {
	public PortfolioProjectInput {
		techStack = techStack == null ? List.of() : List.copyOf(techStack);
	}
}
