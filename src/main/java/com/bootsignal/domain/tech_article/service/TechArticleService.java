package com.bootsignal.domain.tech_article.service;

import com.bootsignal.domain.tech_article.dto.TechArticleResponse;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.entity.TechArticle;
import com.bootsignal.domain.tech_article.repository.TechArticleRepository;
import com.bootsignal.global.dto.PageResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	public PageResponse<TechArticleResponse> getList(ArticleSource source, Pageable pageable) {
		LocalDate latestBatchDate = findLatestBatchDate(source);
		if (latestBatchDate == null) {
			return PageResponse.from(Page.empty(toSortedPageable(pageable)));
		}

		LocalDateTime batchStart = latestBatchDate.atStartOfDay();
		LocalDateTime batchEnd = latestBatchDate.plusDays(1).atStartOfDay();
		Pageable sortedPageable = toSortedPageable(pageable);

		Page<TechArticle> articles = source == null
			? techArticleRepository.findByUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtDesc(
				batchStart, batchEnd, sortedPageable)
			: techArticleRepository.findBySourceAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtDesc(
				source, batchStart, batchEnd, sortedPageable);

		return PageResponse.from(articles.map(TechArticleResponse::from));
	}

	// 가장 최근 수집 배치 날짜 조회
	private LocalDate findLatestBatchDate(ArticleSource source) {
		return techArticleRepository.findMaxUpdatedAt(source)
			.map(LocalDateTime::toLocalDate)
			.orElse(null);
	}

	// 페이지네이션 정렬
	private Pageable toSortedPageable(Pageable pageable) {
		int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
		return PageRequest.of(pageable.getPageNumber(), size, Sort.by(Sort.Direction.DESC, "publishedAt"));
	}
}
