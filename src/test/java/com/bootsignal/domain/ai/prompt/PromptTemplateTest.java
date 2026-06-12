package com.bootsignal.domain.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptTemplateTest {

	@Test
	void renderReplacesVariablesAndKeepsPromptVersion() {
		PromptTemplate template = new PromptTemplate(
			"REVIEW_SUMMARY",
			"v1",
			"과정 {{courseTitle}} 리뷰를 분석한다.",
			"리뷰: {{reviewContent}}"
		);

		RenderedPrompt rendered = template.render(Map.of(
			"courseTitle", "백엔드 부트캠프",
			"reviewContent", "강의가 체계적입니다."
		));

		assertThat(rendered.systemPrompt()).isEqualTo("과정 백엔드 부트캠프 리뷰를 분석한다.");
		assertThat(rendered.userPrompt()).isEqualTo("리뷰: 강의가 체계적입니다.");
		assertThat(rendered.promptVersion()).isEqualTo("REVIEW_SUMMARY:v1");
	}

	@Test
	void renderThrowsWhenVariableIsMissing() {
		PromptTemplate template = new PromptTemplate(
			"REVIEW_SUMMARY",
			"v1",
			"",
			"리뷰: {{reviewContent}}"
		);

		assertThatThrownBy(() -> template.render(Map.of()))
			.isInstanceOf(AiNonRetryableException.class)
			.hasMessage("프롬프트 변수 'reviewContent' 값이 없습니다.");
	}

	@Test
	void registryFindsLatestTemplateByName() {
		PromptTemplate oldTemplate = new PromptTemplate("REVIEW_SUMMARY", "v1", "", "v1 {{content}}");
		PromptTemplate newTemplate = new PromptTemplate("REVIEW_SUMMARY", "v2", "", "v2 {{content}}");
		PromptTemplateRegistry registry = new PromptTemplateRegistry(List.of(oldTemplate, newTemplate));

		assertThat(registry.latest("REVIEW_SUMMARY")).isEqualTo(newTemplate);
		assertThat(registry.get("REVIEW_SUMMARY", "v1")).isEqualTo(oldTemplate);
	}
}
