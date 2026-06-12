package com.bootsignal.domain.ai.prompt;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateRegistry {

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
			.max(Comparator.comparing(PromptTemplate::version))
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
}
