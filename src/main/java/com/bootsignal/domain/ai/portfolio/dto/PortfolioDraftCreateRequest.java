package com.bootsignal.domain.ai.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

// 사용자가 포트폴리오 초안 생성을 위해 직접 제공해야 하는 입력값을 검증한다.
public record PortfolioDraftCreateRequest(
	@NotBlank @Size(max = 100) String targetJob,
	@NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 50) String> skills,
	@NotEmpty @Size(max = 10) List<@Valid PortfolioProjectExperienceRequest> projects,
	@Size(max = 500) String education,
	@Size(max = 1000) String careerSummary,
	PortfolioDraftTone tone
) {
	public PortfolioDraftTone resolvedTone() {
		return tone == null ? PortfolioDraftTone.PROFESSIONAL : tone;
	}
}
