package com.bootsignal.domain.ai.portfolio.controller;

import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftCreateRequest;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftResponse;
import com.bootsignal.domain.ai.portfolio.service.PortfolioDraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
