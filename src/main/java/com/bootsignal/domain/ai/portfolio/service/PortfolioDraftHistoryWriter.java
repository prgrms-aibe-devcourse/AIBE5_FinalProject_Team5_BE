package com.bootsignal.domain.ai.portfolio.service;

import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftContent;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftCreateRequest;
import com.bootsignal.domain.ai.portfolio.entity.PortfolioDraftHistory;
import com.bootsignal.domain.ai.portfolio.repository.PortfolioDraftHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioDraftHistoryWriter {

	private final PortfolioDraftHistoryRepository portfolioDraftHistoryRepository;

	// 외부 트랜잭션과 독립된 새 트랜잭션으로 실행 — 저장 실패 시 외부 트랜잭션에 영향을 주지 않는다.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void save(String executionId, Long userId,
		PortfolioDraftCreateRequest request, PortfolioDraftContent content) {
		portfolioDraftHistoryRepository.save(
			PortfolioDraftHistory.of(executionId, userId, request, content)
		);
	}
}
