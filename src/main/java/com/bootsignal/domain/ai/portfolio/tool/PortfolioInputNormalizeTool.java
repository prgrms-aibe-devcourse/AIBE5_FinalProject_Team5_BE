package com.bootsignal.domain.ai.portfolio.tool;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftTone;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioProjectExperienceRequest;
import com.bootsignal.domain.ai.tool.AgentTool;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PortfolioInputNormalizeTool implements AgentTool {

	// 다양한 입력 객체를 프롬프트에 넣기 쉬운 내부 모델로 정리한다.
	@Override
	public String name() {
		return "portfolio-input-normalize";
	}

	@Override
	public Map<String, Object> execute(AgentExecutionContext context) {
		return Map.of("portfolioInput", normalize(context));
	}

	public PortfolioDraftInput normalize(AgentExecutionContext context) {
		Map<String, Object> input = context.input();
		return new PortfolioDraftInput(
			requiredText(input.get("targetJob"), "목표 직무"),
			requiredTextList(input.get("skills"), "기술 스택"),
			requiredProjectList(input.get("projects")),
			optionalText(input.get("education")),
			optionalText(input.get("careerSummary")),
			resolveToneDescription(input),
			optionalText(input.get("userNickname"))
		);
	}

	private List<String> requiredTextList(Object value, String fieldName) {
		List<String> values = optionalTextList(value, fieldName);
		if (values.isEmpty()) {
			throw invalidInput(fieldName + "은 1개 이상 입력해야 합니다.");
		}
		return values;
	}

	private List<String> optionalTextList(Object value, String fieldName) {
		if (value == null) {
			return List.of();
		}
		if (!(value instanceof Collection<?> collection)) {
			throw invalidInput(fieldName + " 형식이 올바르지 않습니다.");
		}
		return collection.stream()
			.map(item -> requiredText(item, fieldName))
			.distinct()
			.toList();
	}

	private List<PortfolioProjectInput> requiredProjectList(Object value) {
		if (!(value instanceof Collection<?> collection) || collection.isEmpty()) {
			throw invalidInput("프로젝트 경험은 1개 이상 입력해야 합니다.");
		}
		return collection.stream()
			.map(this::toProjectInput)
			.toList();
	}

	private PortfolioProjectInput toProjectInput(Object value) {
		if (value instanceof PortfolioProjectExperienceRequest request) {
			return new PortfolioProjectInput(
				requiredText(request.name(), "프로젝트명"),
				requiredText(request.role(), "프로젝트 역할"),
				requiredText(request.description(), "프로젝트 설명"),
				optionalTextList(request.techStack(), "프로젝트 기술 스택"),
				optionalText(request.achievement()),
				optionalText(request.link())
			);
		}
		if (value instanceof Map<?, ?> map) {
			return new PortfolioProjectInput(
				requiredText(map.get("name"), "프로젝트명"),
				requiredText(map.get("role"), "프로젝트 역할"),
				requiredText(map.get("description"), "프로젝트 설명"),
				optionalTextList(map.get("techStack"), "프로젝트 기술 스택"),
				optionalText(map.get("achievement")),
				optionalText(map.get("link"))
			);
		}
		throw invalidInput("프로젝트 경험 형식이 올바르지 않습니다.");
	}

	private String resolveToneDescription(Map<String, Object> input) {
		String toneDescription = optionalText(input.get("toneDescription"));
		if (StringUtils.hasText(toneDescription)) {
			return toneDescription;
		}
		Object tone = input.get("tone");
		if (tone instanceof PortfolioDraftTone portfolioDraftTone) {
			return portfolioDraftTone.description();
		}
		return PortfolioDraftTone.PROFESSIONAL.description();
	}

	private String requiredText(Object value, String fieldName) {
		String text = optionalText(value);
		if (!StringUtils.hasText(text)) {
			throw invalidInput(fieldName + "은 필수입니다.");
		}
		return text;
	}

	private String optionalText(Object value) {
		return value == null ? "" : String.valueOf(value).strip();
	}

	private AiNonRetryableException invalidInput(String message) {
		return new AiNonRetryableException(ErrorCode.AI_INPUT_INVALID, message);
	}
}
