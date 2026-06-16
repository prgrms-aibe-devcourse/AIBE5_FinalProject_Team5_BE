package com.bootsignal.domain.tech_article.controller;

import com.bootsignal.domain.tech_article.dto.TechArticleCollectResponse;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.service.TechArticleCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tech-articles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTechArticleController {

	private final TechArticleCollectService techArticleCollectService;

	/**
	 * RSS 피드에서 기술 아티클을 수집한다.
	 * source 쿼리 파라미터 미입력 시 등록된 전체 소스를 수집
	 */
	@PostMapping("/collect")
	public TechArticleCollectResponse collect(
		@RequestParam(required = false) ArticleSource source
	) {
		return techArticleCollectService.collect(source);
	}
}
