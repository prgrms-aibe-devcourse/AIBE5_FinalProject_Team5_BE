package com.bootsignal.domain.ai.portfolio.dto;

import com.bootsignal.domain.ai.portfolio.entity.PortfolioDraftHistory;
import java.time.LocalDateTime;
import java.util.List;

public record PortfolioDraftHistoryDetailResponse(
	Long historyId,
	String executionId,
	// 사용자 입력 정보
	String targetJob,
	List<String> skills,
	List<Object> projects,
	String education,
	String careerSummary,
	String tone,
	// AI 생성 초안
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
			history.getSkillList(),
			history.getProjectList(),
			history.getEducation(),
			history.getCareerSummary(),
			history.getTone(),
			history.getIntroduction(),
			history.getCoreCompetencyList(),
			history.getProjectDescriptionList(),
			history.getTechStackSummary(),
			history.getImprovementSuggestionList(),
			history.getCreatedAt()
		);
	}
}
