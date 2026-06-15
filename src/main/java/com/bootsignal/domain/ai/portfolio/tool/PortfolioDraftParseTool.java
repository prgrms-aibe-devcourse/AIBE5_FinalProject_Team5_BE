package com.bootsignal.domain.ai.portfolio.tool;

import com.bootsignal.domain.ai.exception.AiRetryableException;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftProject;
import com.bootsignal.domain.ai.tool.AgentTool;
import com.bootsignal.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PortfolioDraftParseTool implements AgentTool {

	// AI 응답은 JSON만 정상 결과로 인정하고, 파싱 실패는 재시도 가능한 출력 오류로 처리한다.
	private static final Pattern MARKDOWN_JSON_BLOCK = Pattern.compile("(?s)^```(?:json)?\\s*(.*?)\\s*```$");

	private final ObjectMapper objectMapper;

	@Override
	public String name() {
		return "portfolio-draft-parse";
	}

	@Override
	public Map<String, Object> execute(AgentExecutionContext context) {
		Object content = context.input().get("openAiContent");
		if (!(content instanceof String text)) {
			throw invalidOutput("OpenAI 응답 내용이 없습니다.");
		}
		return Map.of("draft", parse(text));
	}

	public PortfolioDraftContent parse(String content) {
		try {
			PortfolioDraftContent draft = objectMapper.readValue(extractJson(content), PortfolioDraftContent.class);
			validate(draft);
			return draft;
		} catch (JsonProcessingException exception) {
			throw new AiRetryableException(
				ErrorCode.AI_OUTPUT_INVALID,
				"포트폴리오 초안 JSON 파싱에 실패했습니다.",
				exception
			);
		}
	}

	private String extractJson(String content) {
		if (!StringUtils.hasText(content)) {
			throw invalidOutput("OpenAI 응답 내용이 비어 있습니다.");
		}
		String stripped = content.strip();
		Matcher matcher = MARKDOWN_JSON_BLOCK.matcher(stripped);
		if (matcher.matches()) {
			return matcher.group(1).strip();
		}

		int start = stripped.indexOf('{');
		int end = stripped.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return stripped.substring(start, end + 1).strip();
		}
		return stripped;
	}

	private void validate(PortfolioDraftContent draft) {
		if (!StringUtils.hasText(draft.introduction())) {
			throw invalidOutput("포트폴리오 소개 문장이 비어 있습니다.");
		}
		if (draft.coreCompetencies().isEmpty()) {
			throw invalidOutput("포트폴리오 핵심 역량이 비어 있습니다.");
		}
		if (draft.projectDescriptions().isEmpty()) {
			throw invalidOutput("포트폴리오 프로젝트 설명이 비어 있습니다.");
		}
		if (!StringUtils.hasText(draft.techStackSummary())) {
			throw invalidOutput("포트폴리오 기술 스택 요약이 비어 있습니다.");
		}
		for (PortfolioDraftProject project : draft.projectDescriptions()) {
			validateProject(project);
		}
	}

	private void validateProject(PortfolioDraftProject project) {
		if (!StringUtils.hasText(project.name())
			|| !StringUtils.hasText(project.summary())
			|| !StringUtils.hasText(project.role())) {
			throw invalidOutput("포트폴리오 프로젝트 필수 항목이 비어 있습니다.");
		}
	}

	private AiRetryableException invalidOutput(String message) {
		return new AiRetryableException(ErrorCode.AI_OUTPUT_INVALID, message);
	}
}
