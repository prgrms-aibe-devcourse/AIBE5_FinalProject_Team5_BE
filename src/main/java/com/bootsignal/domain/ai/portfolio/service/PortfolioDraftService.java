package com.bootsignal.domain.ai.portfolio.service;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.harness.AgentHarness;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftCreateRequest;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftResponse;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioDraftService {

	// 인증된 사용자 정보를 실행 컨텍스트에 담아 포트폴리오 Agent 실행을 요청한다.
	private final AgentHarness agentHarness;
	private final UserRepository userRepository;

	public PortfolioDraftResponse createDraft(PortfolioDraftCreateRequest request) {
		User user = getAuthenticatedUser();
		AgentExecutionResult result = agentHarness.execute(AgentExecutionContext.of(
			AgentType.PORTFOLIO_DRAFT,
			user.getId(),
			toInputSummary(request),
			toInput(user, request)
		));

		if (!result.isSuccess()) {
			throw new BootSignalException(ErrorCode.AI_EXECUTION_FAILED, result.errorMessage());
		}

		Object draft = result.output().get("draft");
		if (!(draft instanceof PortfolioDraftContent content)) {
			throw new BootSignalException(ErrorCode.AI_OUTPUT_INVALID, "포트폴리오 초안 결과를 찾을 수 없습니다.");
		}
		return PortfolioDraftResponse.from(result.executionId(), content);
	}

	private Map<String, Object> toInput(User user, PortfolioDraftCreateRequest request) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("targetJob", request.targetJob());
		input.put("skills", request.skills());
		input.put("projects", request.projects());
		input.put("education", request.education());
		input.put("careerSummary", request.careerSummary());
		input.put("tone", request.resolvedTone());
		input.put("toneDescription", request.resolvedTone().description());
		input.put("userNickname", user.getNickname());
		return input;
	}

	private String toInputSummary(PortfolioDraftCreateRequest request) {
		return "포트폴리오 초안 생성 요청 - 목표 직무: "
			+ request.targetJob()
			+ ", 기술 "
			+ request.skills().size()
			+ "개, 프로젝트 "
			+ request.projects().size()
			+ "개";
	}

	private User getAuthenticatedUser() {
		String email = SecurityUtil.getCurrentUserEmail();
		return userRepository.findByEmail(email)
			.filter(user -> !user.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
	}
}
