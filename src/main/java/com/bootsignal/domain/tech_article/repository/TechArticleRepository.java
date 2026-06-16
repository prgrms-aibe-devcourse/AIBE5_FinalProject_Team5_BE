package com.bootsignal.domain.tech_article.repository;

import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.entity.TechArticle;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechArticleRepository extends JpaRepository<TechArticle, Long> {

	Optional<TechArticle> findBySourceAndRssGuid(ArticleSource source, String rssGuid);

	@Query("""
		SELECT MAX(t.updatedAt) FROM TechArticle t
		WHERE (:source IS NULL OR t.source = :source)
		""")
	Optional<LocalDateTime> findMaxUpdatedAt(@Param("source") ArticleSource source);

	Page<TechArticle> findByUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtDesc(
		LocalDateTime startInclusive,
		LocalDateTime endExclusive,
		Pageable pageable
	);

	Page<TechArticle> findBySourceAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtDesc(
		ArticleSource source,
		LocalDateTime startInclusive,
		LocalDateTime endExclusive,
		Pageable pageable
	);
}
