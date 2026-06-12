package com.bootsignal.domain.ai.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

// 프로젝트 경험 입력은 사실 기반 초안 생성을 위한 핵심 근거 데이터다.
public record PortfolioProjectExperienceRequest(
	@NotBlank @Size(max = 100) String name,
	@NotBlank @Size(max = 100) String role,
	@NotBlank @Size(max = 1000) String description,
	@Size(max = 20) List<@NotBlank @Size(max = 50) String> techStack,
	@Size(max = 1000) String achievement,
	@Size(max = 500) String link
) {
}
