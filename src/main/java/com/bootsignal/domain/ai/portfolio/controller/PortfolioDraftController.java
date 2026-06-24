package com.bootsignal.domain.ai.portfolio.controller;

import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftCreateRequest;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftHistoryDetailResponse;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftHistoryResponse;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftResponse;
import com.bootsignal.domain.ai.portfolio.service.PortfolioDraftService;
import com.bootsignal.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/portfolio-drafts")
@RequiredArgsConstructor
public class PortfolioDraftController {

	// 로그인 사용자의 포트폴리오 초안 생성 요청을 서비스 계층으로 위임한다.
	private final PortfolioDraftService portfolioDraftService;

	@PostMapping
	public PortfolioDraftResponse createDraft(@RequestBody @Valid PortfolioDraftCreateRequest request) {
		return portfolioDraftService.createDraft(request);
	}

	@GetMapping("/history")
	public PageResponse<PortfolioDraftHistoryResponse> getHistory(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		return portfolioDraftService.getHistory(
			PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
		);
	}

	@GetMapping("/history/{historyId}")
	public PortfolioDraftHistoryDetailResponse getHistoryDetail(@PathVariable Long historyId) {
		return portfolioDraftService.getHistoryDetail(historyId);
	}

	@DeleteMapping("/history/{historyId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteHistory(@PathVariable Long historyId) {
		portfolioDraftService.deleteHistory(historyId);
	}
}
