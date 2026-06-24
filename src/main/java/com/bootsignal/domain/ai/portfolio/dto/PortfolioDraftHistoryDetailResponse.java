package com.bootsignal.domain.ai.portfolio.dto;

import com.bootsignal.domain.ai.portfolio.entity.PortfolioDraftHistory;
import java.time.LocalDateTime;
import java.util.List;

public record PortfolioDraftHistoryDetailResponse(
	Long historyId,
	String executionId,
	String targetJob,
	List<String> skills,
	List<PortfolioProjectExperienceRequest> projects,
	String education,
	String careerSummary,
	String tone,
	String introduction,
	List<String> coreCompetencies,
	List<PortfolioDraftProject> projectDescriptions,
	String techStackSummary,
	List<String> improvementSuggestions,
	LocalDateTime createdAt
) {
	public static PortfolioDraftHistoryDetailResponse from(PortfolioDraftHistory history) {
		return new PortfolioDraftHistoryDetailResponse(
			history.getId(),
			history.getExecutionId(),
			history.getTargetJob(),
			history.getSkills(),
			history.getProjects(),
			history.getEducation(),
			history.getCareerSummary(),
			history.getTone(),
			history.getIntroduction(),
			history.getCoreCompetencies(),
			history.getProjectDescriptions(),
			history.getTechStackSummary(),
			history.getImprovementSuggestions(),
			history.getCreatedAt()
		);
	}
}
