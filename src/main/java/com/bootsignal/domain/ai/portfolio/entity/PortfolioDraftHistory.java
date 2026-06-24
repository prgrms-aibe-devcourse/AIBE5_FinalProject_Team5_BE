package com.bootsignal.domain.ai.portfolio.entity;

import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftCreateRequest;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftProject;
import com.bootsignal.global.entity.BaseEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
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

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "execution_id", nullable = false, length = 36)
	private String executionId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "target_job", nullable = false, length = 100)
	private String targetJob;

	@Column(name = "skills", nullable = false, columnDefinition = "text")
	private String skills;

	@Column(name = "projects", nullable = false, columnDefinition = "text")
	private String projects;

	@Column(name = "education", length = 500)
	private String education;

	@Column(name = "career_summary", columnDefinition = "text")
	private String careerSummary;

	@Column(name = "tone", nullable = false, length = 20)
	private String tone;

	@Column(name = "introduction", columnDefinition = "text")
	private String introduction;

	@Column(name = "core_competencies", columnDefinition = "text")
	private String coreCompetencies;

	@Column(name = "project_descriptions", columnDefinition = "text")
	private String projectDescriptions;

	@Column(name = "tech_stack_summary", columnDefinition = "text")
	private String techStackSummary;

	@Column(name = "improvement_suggestions", columnDefinition = "text")
	private String improvementSuggestions;

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
		history.skills = toJson(request.skills());
		history.projects = toJson(request.projects());
		history.education = request.education();
		history.careerSummary = request.careerSummary();
		history.tone = request.resolvedTone().name();
		history.introduction = content.introduction();
		history.coreCompetencies = toJson(content.coreCompetencies());
		history.projectDescriptions = toJson(content.projectDescriptions());
		history.techStackSummary = content.techStackSummary();
		history.improvementSuggestions = toJson(content.improvementSuggestions());
		return history;
	}

	public List<String> getSkillList() {
		return fromJson(skills, new TypeReference<>() {});
	}

	public List<Object> getProjectList() {
		return fromJson(projects, new TypeReference<>() {});
	}

	public List<String> getCoreCompetencyList() {
		return fromJson(coreCompetencies, new TypeReference<>() {});
	}

	public List<PortfolioDraftProject> getProjectDescriptionList() {
		return fromJson(projectDescriptions, new TypeReference<>() {});
	}

	public List<String> getImprovementSuggestionList() {
		return fromJson(improvementSuggestions, new TypeReference<>() {});
	}

	private static String toJson(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("JSON 직렬화 실패", e);
		}
	}

	private static <T> T fromJson(String json, TypeReference<T> type) {
		if (json == null) {
			return null;
		}
		try {
			return MAPPER.readValue(json, type);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("JSON 역직렬화 실패", e);
		}
	}
}
