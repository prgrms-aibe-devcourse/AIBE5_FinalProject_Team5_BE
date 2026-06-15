package com.bootsignal.domain.ai.portfolio.dto;

import java.util.List;

// 포트폴리오 초안에 포함될 프로젝트별 설명과 강조점을 표현한다.
public record PortfolioDraftProject(
	String name,
	String summary,
	String role,
	List<String> techStack,
	List<String> highlights
) {
	public PortfolioDraftProject {
		techStack = techStack == null ? List.of() : List.copyOf(techStack);
		highlights = highlights == null ? List.of() : List.copyOf(highlights);
	}
}
