package com.bootsignal.domain.ai.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.harness.AgentHarness;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryContent;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryCreateRequest;
import com.bootsignal.domain.ai.review.dto.ReviewSummaryResponse;
import com.bootsignal.domain.ai.review.repository.ReviewSummaryCacheRepository;
import com.bootsignal.domain.crawled_review.repository.CrawledReviewRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryServiceTest {

	@Mock
	private AgentHarness agentHarness;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ReviewSummaryCacheRepository cacheRepository;

	@Mock
	private CrawledReviewRepository crawledReviewRepository;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createSummaryRunsReviewSummaryAgentForAuthenticatedUser() {
		User user = User.signupLocal("reviewer@example.com", "encoded-password", "reviewer");
		setAuthentication(user.getEmail());
		ReviewSummaryService service = new ReviewSummaryService(agentHarness, userRepository, cacheRepository, crawledReviewRepository);

		ReviewSummaryContent content = new ReviewSummaryContent(
			"프로젝트와 멘토링 만족도가 높습니다.",
			List.of("멘토링"),
			List.of("학습량이 많음"),
			List.of("프로젝트 경험을 원하는 학습자"),
			List.of("프로젝트")
		);

		Object[] snapshot = new Object[]{10L, Instant.now()};
		given(crawledReviewRepository.findReviewSnapshotByCourseId(eq(1L))).willReturn(Optional.of(snapshot));
		given(cacheRepository.findByCourseId(eq(1L))).willReturn(Optional.empty());
		given(userRepository.findByEmail("reviewer@example.com")).willReturn(Optional.of(user));
		given(agentHarness.execute(any())).willAnswer(invocation -> AgentExecutionResult.success(
			invocation.getArgument(0),
			"수강후기 요약 완료",
			Map.of(
				"courseId", 1L,
				"courseTitle", "백엔드 과정",
				"reviewCount", 10,
				"averageRating", BigDecimal.valueOf(4.5),
				"summary", content
			)
		));

		ReviewSummaryResponse response = service.createSummary(new ReviewSummaryCreateRequest(1L, 30));

		assertThat(response.summary()).isEqualTo("프로젝트와 멘토링 만족도가 높습니다.");
		assertThat(response.courseTitle()).isEqualTo("백엔드 과정");
		ArgumentCaptor<AgentExecutionContext> captor = ArgumentCaptor.forClass(AgentExecutionContext.class);
		verify(agentHarness).execute(captor.capture());
		AgentExecutionContext context = captor.getValue();
		assertThat(context.agentType()).isEqualTo(AgentType.REVIEW_SUMMARY);
		assertThat(context.inputSummary()).contains("과정 ID: 1");
		assertThat(context.input()).containsEntry("courseId", 1L);
		assertThat(context.input()).containsEntry("maxReviewCount", 30);
	}

	@Test
	void createSummaryThrowsUnauthorizedWithoutAuthentication() {
		ReviewSummaryService service = new ReviewSummaryService(agentHarness, userRepository, cacheRepository, crawledReviewRepository);

		assertThatThrownBy(() -> service.createSummary(new ReviewSummaryCreateRequest(1L, null)))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);

		verify(userRepository, never()).findByEmail(any());
		verify(agentHarness, never()).execute(any());
	}

	private void setAuthentication(String email) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(email, null, List.of())
		);
	}
}
