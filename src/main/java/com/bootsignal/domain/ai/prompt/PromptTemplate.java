package com.bootsignal.domain.ai.prompt;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public record PromptTemplate(
	String name,
	String version,
	String systemTemplate,
	String userTemplate
) {

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

	public PromptTemplate {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("프롬프트 템플릿 이름은 필수입니다.");
		}
		if (!StringUtils.hasText(version)) {
			throw new IllegalArgumentException("프롬프트 템플릿 버전은 필수입니다.");
		}
		if (!StringUtils.hasText(userTemplate)) {
			throw new IllegalArgumentException("사용자 프롬프트 템플릿은 필수입니다.");
		}

		name = name.strip();
		version = version.strip();
		systemTemplate = StringUtils.hasText(systemTemplate) ? systemTemplate.strip() : "";
		userTemplate = userTemplate.strip();
	}

	public RenderedPrompt render(Map<String, ?> variables) {
		Map<String, ?> safeVariables = variables == null ? Map.of() : variables;
		return new RenderedPrompt(
			name,
			version,
			replaceVariables(systemTemplate, safeVariables),
			replaceVariables(userTemplate, safeVariables)
		);
	}

	private String replaceVariables(String template, Map<String, ?> variables) {
		if (!StringUtils.hasText(template)) {
			return "";
		}

		Matcher matcher = VARIABLE_PATTERN.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String key = matcher.group(1);
			if (!variables.containsKey(key) || variables.get(key) == null) {
				throw new AiNonRetryableException(
					ErrorCode.AI_INPUT_INVALID,
					"프롬프트 변수 '" + key + "' 값이 없습니다."
				);
			}
			matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(variables.get(key))));
		}
		matcher.appendTail(result);
		return result.toString().strip();
	}
}
