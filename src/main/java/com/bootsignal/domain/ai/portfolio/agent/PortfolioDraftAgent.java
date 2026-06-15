package com.bootsignal.domain.ai.portfolio.agent;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.agent.AiAgent;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.openai.OpenAiClient;
import com.bootsignal.domain.ai.openai.OpenAiRequest;
import com.bootsignal.domain.ai.openai.OpenAiResponse;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.tool.PortfolioDraftInput;
import com.bootsignal.domain.ai.portfolio.tool.PortfolioDraftParseTool;
import com.bootsignal.domain.ai.portfolio.tool.PortfolioInputNormalizeTool;
import com.bootsignal.domain.ai.portfolio.tool.PortfolioPromptRenderTool;
import com.bootsignal.domain.ai.prompt.RenderedPrompt;
import com.bootsignal.global.config.properties.OpenAiProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioDraftAgent implements AiAgent {

	// 포트폴리오 초안 생성은 입력 정규화, 프롬프트 렌더링, AI 호출, JSON 파싱 순서로 처리한다.
	private static final double TEMPERATURE = 0.2;

	private final PortfolioInputNormalizeTool normalizeTool;
	private final PortfolioPromptRenderTool promptRenderTool;
	private final PortfolioDraftParseTool parseTool;
	private final OpenAiClient openAiClient;
	private final OpenAiProperties openAiProperties;

	@Override
	public AgentType type() {
		return AgentType.PORTFOLIO_DRAFT;
	}

	@Override
	public AgentExecutionResult execute(AgentExecutionContext context) {
		PortfolioDraftInput input = normalizeTool.normalize(context);
		RenderedPrompt prompt = promptRenderTool.render(input);
		OpenAiRequest request = new OpenAiRequest(
			openAiProperties.model(),
			prompt.systemPrompt(),
			prompt.userPrompt(),
			prompt.promptVersion(),
			TEMPERATURE,
			openAiProperties.maxOutputTokens()
		);

		OpenAiResponse response = openAiClient.complete(request);
		PortfolioDraftContent draft = parseTool.parse(response.content());

		return AgentExecutionResult.success(
			context,
			input.targetJob() + " 포트폴리오 초안 생성 완료",
			Map.of("draft", draft),
			response.toExecutionMetadata(request.promptVersion(), request.temperature())
		);
	}
}
