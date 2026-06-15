package com.bootsignal.domain.ai.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.harness.AgentHarness;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftCreateRequest;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftProject;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftResponse;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftTone;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioProjectExperienceRequest;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
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
class PortfolioDraftServiceTest {

	// Service는 인증 사용자 확인 후 Agent 실행 컨텍스트를 정확히 구성해야 한다.
	@Mock
	private AgentHarness agentHarness;

	@Mock
	private UserRepository userRepository;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createDraftRunsPortfolioAgentForAuthenticatedUser() {
		User user = User.signupLocal("writer@example.com", "encoded-password", "writer");
		setAuthentication(user.getEmail());
		PortfolioDraftService service = new PortfolioDraftService(agentHarness, userRepository);
		PortfolioDraftContent content = new PortfolioDraftContent(
			"백엔드 개발자 포트폴리오 소개입니다.",
			List.of("API 구현"),
			List.of(new PortfolioDraftProject(
				"BootSignal",
				"교육 과정 탐색 서비스입니다.",
				"백엔드 개발",
				List.of("Java"),
				List.of("검색 API 구현")
			)),
			"Java 기반 API 구현 경험이 있습니다.",
			List.of("성과 지표를 보완하세요.")
		);
		given(userRepository.findByEmail("writer@example.com")).willReturn(Optional.of(user));
		given(agentHarness.execute(any())).willAnswer(invocation -> AgentExecutionResult.success(
			invocation.getArgument(0),
			"포트폴리오 초안 생성 완료",
			Map.of("draft", content)
		));

		PortfolioDraftResponse response = service.createDraft(request());

		assertThat(response.introduction()).isEqualTo("백엔드 개발자 포트폴리오 소개입니다.");
		assertThat(response.projectDescriptions()).hasSize(1);
		ArgumentCaptor<AgentExecutionContext> captor = ArgumentCaptor.forClass(AgentExecutionContext.class);
		verify(agentHarness).execute(captor.capture());
		AgentExecutionContext context = captor.getValue();
		assertThat(context.agentType()).isEqualTo(AgentType.PORTFOLIO_DRAFT);
		assertThat(context.inputSummary()).contains("목표 직무: 백엔드 개발자");
		assertThat(context.input()).containsEntry("targetJob", "백엔드 개발자");
		assertThat(context.input()).containsEntry("tone", PortfolioDraftTone.PROFESSIONAL);
	}

	@Test
	void createDraftThrowsUnauthorizedWithoutAuthentication() {
		PortfolioDraftService service = new PortfolioDraftService(agentHarness, userRepository);

		assertThatThrownBy(() -> service.createDraft(request()))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);

		verify(userRepository, never()).findByEmail(any());
		verify(agentHarness, never()).execute(any());
	}

	private PortfolioDraftCreateRequest request() {
		return new PortfolioDraftCreateRequest(
			"백엔드 개발자",
			List.of("Java", "Spring Boot"),
			List.of(new PortfolioProjectExperienceRequest(
				"BootSignal",
				"백엔드 개발",
				"교육 과정 탐색 서비스를 구현했습니다.",
				List.of("Java", "Spring Boot"),
				"검색 API를 구현했습니다.",
				null
			)),
			"백엔드 부트캠프 수료",
			"팀 프로젝트 경험 보유",
			PortfolioDraftTone.PROFESSIONAL
		);
	}

	private void setAuthentication(String email) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(email, null, List.of())
		);
	}
}
