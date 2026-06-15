package com.bootsignal.domain.ai.prompt;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateRegistry {

	// 같은 이름의 프롬프트가 여러 버전일 때 숫자 버전 기준으로 최신 템플릿을 선택한다.
	private static final Pattern SIMPLE_VERSION_PATTERN = Pattern.compile("^[vV]?(\\d+)$");

	private final Map<String, PromptTemplate> templates;

	public PromptTemplateRegistry(List<PromptTemplate> templates) {
		this.templates = templates.stream()
			.collect(Collectors.toUnmodifiableMap(this::key, Function.identity()));
	}

	public PromptTemplate get(String name, String version) {
		PromptTemplate template = templates.get(key(name, version));
		if (template == null) {
			throw new AiNonRetryableException(
				ErrorCode.AI_INPUT_INVALID,
				"프롬프트 템플릿을 찾을 수 없습니다. name=" + name + ", version=" + version
			);
		}
		return template;
	}

	public PromptTemplate latest(String name) {
		return templates.values().stream()
			.filter(template -> template.name().equals(name))
			.max((left, right) -> compareVersion(left.version(), right.version()))
			.orElseThrow(() -> new AiNonRetryableException(
				ErrorCode.AI_INPUT_INVALID,
				"프롬프트 템플릿을 찾을 수 없습니다. name=" + name
			));
	}

	private String key(PromptTemplate template) {
		return key(template.name(), template.version());
	}

	private String key(String name, String version) {
		return name + ":" + version;
	}

	private int compareVersion(String left, String right) {
		Integer leftNumber = parseSimpleVersion(left);
		Integer rightNumber = parseSimpleVersion(right);
		if (leftNumber != null && rightNumber != null) {
			return Integer.compare(leftNumber, rightNumber);
		}
		return left.compareTo(right);
	}

	private Integer parseSimpleVersion(String version) {
		Matcher matcher = SIMPLE_VERSION_PATTERN.matcher(version);
		if (!matcher.matches()) {
			return null;
		}
		return Integer.valueOf(matcher.group(1));
	}
}
