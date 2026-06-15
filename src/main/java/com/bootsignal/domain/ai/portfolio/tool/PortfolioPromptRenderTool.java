package com.bootsignal.domain.ai.portfolio.tool;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.prompt.PromptTemplate;
import com.bootsignal.domain.ai.prompt.PromptTemplateRegistry;
import com.bootsignal.domain.ai.prompt.RenderedPrompt;
import com.bootsignal.domain.ai.tool.AgentTool;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PortfolioPromptRenderTool implements AgentTool {

	// 최신 포트폴리오 프롬프트 템플릿에 정규화된 입력을 주입한다.
	private final PromptTemplateRegistry promptTemplateRegistry;
	private final PortfolioInputNormalizeTool normalizeTool;

	@Override
	public String name() {
		return "portfolio-prompt-render";
	}

	@Override
	public Map<String, Object> execute(AgentExecutionContext context) {
		return Map.of("renderedPrompt", render(normalizeTool.normalize(context)));
	}

	public RenderedPrompt render(PortfolioDraftInput input) {
		PromptTemplate template = promptTemplateRegistry.latest(AgentType.PORTFOLIO_DRAFT.name());
		return template.render(Map.of(
			"targetJob", input.targetJob(),
			"skills", String.join(", ", input.skills()),
			"projectExperience", formatProjects(input),
			"education", defaultText(input.education()),
			"careerSummary", defaultText(input.careerSummary()),
			"tone", input.toneDescription()
		));
	}

	private String formatProjects(PortfolioDraftInput input) {
		return input.projects().stream()
			.map(this::formatProject)
			.collect(Collectors.joining("\n"));
	}

	private String formatProject(PortfolioProjectInput project) {
		return """
			- 프로젝트명: %s
			  역할: %s
			  설명: %s
			  사용 기술: %s
			  성과: %s
			  링크: %s
			""".formatted(
			project.name(),
			project.role(),
			project.description(),
			project.techStack().isEmpty() ? "제공 없음" : String.join(", ", project.techStack()),
			defaultText(project.achievement()),
			defaultText(project.link())
		).strip();
	}

	private String defaultText(String value) {
		return StringUtils.hasText(value) ? value : "제공 없음";
	}
}
