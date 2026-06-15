package com.bootsignal.domain.ai.review.agent;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.agent.AiAgent;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.openai.OpenAiClient;
import com.bootsignal.domain.ai.openai.OpenAiRequest;
import com.bootsignal.domain.ai.openai.OpenAiResponse;
import com.bootsignal.domain.ai.prompt.RenderedPrompt;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryContent;
import com.bootsignal.domain.ai.review.tool.CrawledReviewLoadTool;
import com.bootsignal.domain.ai.review.tool.ReviewSummaryInput;
import com.bootsignal.domain.ai.review.tool.ReviewSummaryParseTool;
import com.bootsignal.domain.ai.review.tool.ReviewSummaryPromptRenderTool;
import com.bootsignal.global.config.properties.OpenAiProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewSummaryAgent implements AiAgent {

	// 고용24 수강후기 요약은 크롤링 데이터 조회, 프롬프트 생성, AI 호출, JSON 파싱 순서로 처리한다.
	private static final double TEMPERATURE = 0.2;

	private final CrawledReviewLoadTool reviewLoadTool;
	private final ReviewSummaryPromptRenderTool promptRenderTool;
	private final ReviewSummaryParseTool parseTool;
	private final OpenAiClient openAiClient;
	private final OpenAiProperties openAiProperties;

	@Override
	public AgentType type() {
		return AgentType.REVIEW_SUMMARY;
	}

	@Override
	public AgentExecutionResult execute(AgentExecutionContext context) {
		ReviewSummaryInput input = reviewLoadTool.load(context);
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
		ReviewSummaryContent summary = parseTool.parse(response.content());

		return AgentExecutionResult.success(
			context,
			input.courseTitle() + " 수강후기 요약 완료",
			output(input, summary),
			response.toExecutionMetadata(request.promptVersion(), request.temperature())
		);
	}

	private Map<String, Object> output(ReviewSummaryInput input, ReviewSummaryContent summary) {
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("courseId", input.courseId());
		output.put("courseTitle", input.courseTitle());
		output.put("reviewCount", input.reviewCount());
		output.put("averageRating", input.averageRating());
		output.put("summary", summary);
		return output;
	}
}
