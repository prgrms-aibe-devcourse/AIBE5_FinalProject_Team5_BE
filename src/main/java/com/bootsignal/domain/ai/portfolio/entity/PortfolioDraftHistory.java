package com.bootsignal.domain.ai.portfolio.entity;

import com.bootsignal.domain.ai.portfolio.converter.PortfolioDraftProjectListConverter;
import com.bootsignal.domain.ai.portfolio.converter.PortfolioProjectExperienceRequestListConverter;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftCreateRequest;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftProject;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioProjectExperienceRequest;
import com.bootsignal.global.converter.StringListConverter;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "portfolio_draft_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioDraftHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "execution_id", nullable = false, length = 36)
	private String executionId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "target_job", nullable = false, length = 100)
	private String targetJob;

	@Convert(converter = StringListConverter.class)
	@Column(name = "skills", nullable = false, columnDefinition = "text")
	private List<String> skills;

	@Convert(converter = PortfolioProjectExperienceRequestListConverter.class)
	@Column(name = "projects", nullable = false, columnDefinition = "text")
	private List<PortfolioProjectExperienceRequest> projects;

	@Column(name = "education", length = 500)
	private String education;

	@Column(name = "career_summary", columnDefinition = "text")
	private String careerSummary;

	@Column(name = "tone", nullable = false, length = 20)
	private String tone;

	@Column(name = "introduction", columnDefinition = "text")
	private String introduction;

	@Convert(converter = StringListConverter.class)
	@Column(name = "core_competencies", columnDefinition = "text")
	private List<String> coreCompetencies;

	@Convert(converter = PortfolioDraftProjectListConverter.class)
	@Column(name = "project_descriptions", columnDefinition = "text")
	private List<PortfolioDraftProject> projectDescriptions;

	@Column(name = "tech_stack_summary", columnDefinition = "text")
	private String techStackSummary;

	@Convert(converter = StringListConverter.class)
	@Column(name = "improvement_suggestions", columnDefinition = "text")
	private List<String> improvementSuggestions;

	public static PortfolioDraftHistory of(
		String executionId,
		Long userId,
		PortfolioDraftCreateRequest request,
		PortfolioDraftContent content
	) {
		PortfolioDraftHistory history = new PortfolioDraftHistory();
		history.executionId = executionId;
		history.userId = userId;
		history.targetJob = request.targetJob();
		history.skills = request.skills();
		history.projects = request.projects();
		history.education = request.education();
		history.careerSummary = request.careerSummary();
		history.tone = request.resolvedTone().name();
		history.introduction = content.introduction();
		history.coreCompetencies = content.coreCompetencies();
		history.projectDescriptions = content.projectDescriptions();
		history.techStackSummary = content.techStackSummary();
		history.improvementSuggestions = content.improvementSuggestions();
		return history;
	}
}
