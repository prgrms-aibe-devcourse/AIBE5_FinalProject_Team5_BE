package com.bootsignal.domain.tech_article.controller;

import com.bootsignal.domain.tech_article.dto.TechArticleResponse;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.service.TechArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class TechArticleController {

	private final TechArticleService techArticleService;

	/**
	 * 기술 아티클 목록 조회 — updatedAt 최신순.
	 *
	 * source 쿼리 파라미터 YOZM | KAKAO_TECH (미입력 시 전체)
	 */
	@GetMapping
	public Page<TechArticleResponse> getList(
		@RequestParam(required = false) ArticleSource source,
		@PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return techArticleService.getList(source, pageable);
	}
}
