package com.bootsignal.domain.ai.portfolio.service;

import com.bootsignal.domain.ai.agent.AgentType;
import com.bootsignal.domain.ai.harness.AgentExecutionContext;
import com.bootsignal.domain.ai.harness.AgentExecutionResult;
import com.bootsignal.domain.ai.harness.AgentHarness;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftCreateRequest;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftHistoryDetailResponse;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftHistoryResponse;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftResponse;
import com.bootsignal.domain.ai.portfolio.entity.PortfolioDraftHistory;
import com.bootsignal.domain.ai.portfolio.repository.PortfolioDraftHistoryRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.dto.PageResponse;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioDraftService {

	private final AgentHarness agentHarness;
	private final UserRepository userRepository;
	private final PortfolioDraftHistoryRepository portfolioDraftHistoryRepository;
	private final PortfolioDraftHistoryWriter portfolioDraftHistoryWriter;

	@Transactional
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

		try {
			portfolioDraftHistoryWriter.save(result.executionId().toString(), user.getId(), request, content);
		} catch (Exception e) {
			log.warn("포트폴리오 이력 저장 실패 executionId={}", result.executionId(), e);
		}

		return PortfolioDraftResponse.from(result.executionId(), content);
	}

	public PageResponse<PortfolioDraftHistoryResponse> getHistory(Pageable pageable) {
		User user = getAuthenticatedUser();
		return PageResponse.from(
			portfolioDraftHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
				.map(PortfolioDraftHistoryResponse::from)
		);
	}

	public PortfolioDraftHistoryDetailResponse getHistoryDetail(Long historyId) {
		User user = getAuthenticatedUser();
		PortfolioDraftHistory history = portfolioDraftHistoryRepository
			.findByIdAndUserId(historyId, user.getId())
			.orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND));
		return PortfolioDraftHistoryDetailResponse.from(history);
	}

	@Transactional
	public void deleteHistory(Long historyId) {
		User user = getAuthenticatedUser();
		PortfolioDraftHistory history = portfolioDraftHistoryRepository
			.findByIdAndUserId(historyId, user.getId())
			.orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND));
		portfolioDraftHistoryRepository.delete(history);
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
