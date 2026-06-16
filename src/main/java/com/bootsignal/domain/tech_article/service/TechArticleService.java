package com.bootsignal.domain.tech_article.service;

import com.bootsignal.domain.tech_article.dto.TechArticleResponse;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.entity.TechArticle;
import com.bootsignal.domain.tech_article.repository.TechArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechArticleService {

	private static final int MAX_PAGE_SIZE = 50;

	private final TechArticleRepository techArticleRepository;

	/* 기술 아티클 목록 조회 */
	public Page<TechArticleResponse> getList(ArticleSource source, Pageable pageable) {
		Pageable sortedPageable = toSortedPageable(pageable);

		// source 쿼리 파라미터 기반 조회
		Page<TechArticle> articles = source == null
			? techArticleRepository.findAllByOrderByUpdatedAtDesc(sortedPageable)
			: techArticleRepository.findBySourceOrderByUpdatedAtDesc(source, sortedPageable);

		return articles.map(TechArticleResponse::from);
	}

	// 페이지네이션 정렬 
	private Pageable toSortedPageable(Pageable pageable) {
		int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
		return PageRequest.of(pageable.getPageNumber(), size, Sort.by(Sort.Direction.DESC, "updatedAt"));
	}
}
