package com.bootsignal.domain.ai.review.tool;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.prompt.PromptTemplate;
import com.bootsignal.domain.ai.prompt.PromptTemplateRegistry;
import com.bootsignal.domain.ai.prompt.RenderedPrompt;
import com.bootsignal.domain.ai.tool.AgentTool;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewSummaryPromptRenderTool implements AgentTool {

	// 최신 리뷰 요약 프롬프트에 과정 정보와 후기 목록을 주입한다.
	private static final int MAX_REVIEW_CONTENT_LENGTH = 500;

	private final PromptTemplateRegistry promptTemplateRegistry;
	private final CrawledReviewLoadTool reviewLoadTool;

	@Override
	public String name() {
		return "review-summary-prompt-render";
	}

	@Override
	public Map<String, Object> execute(AgentExecutionContext context) {
		return Map.of("renderedPrompt", render(reviewLoadTool.load(context)));
	}

	public RenderedPrompt render(ReviewSummaryInput input) {
		PromptTemplate template = promptTemplateRegistry.latest(AgentType.REVIEW_SUMMARY.name());
		return template.render(Map.of(
			"courseTitle", input.courseTitle(),
			"reviewCount", input.reviewCount(),
			"averageRating", input.averageRating(),
			"reviewContent", formatReviews(input)
		));
	}

	private String formatReviews(ReviewSummaryInput input) {
		AtomicInteger index = new AtomicInteger(1);
		return input.reviews().stream()
			.map(review -> formatReview(index.getAndIncrement(), review))
			.collect(Collectors.joining("\n"));
	}

	private String formatReview(int index, CrawledReviewSnippet review) {
		String rating = review.rating() == null ? "평점 없음" : review.rating() + "점";
		return "- 후기 %d / %s: %s".formatted(index, rating, truncate(review.content()));
	}

	private String truncate(String content) {
		if (content == null) {
			return "";
		}
		String stripped = content.strip();
		if (stripped.length() <= MAX_REVIEW_CONTENT_LENGTH) {
			return stripped;
		}
		return stripped.substring(0, MAX_REVIEW_CONTENT_LENGTH) + "...";
	}
}
