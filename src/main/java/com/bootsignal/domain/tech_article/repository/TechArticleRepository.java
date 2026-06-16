package com.bootsignal.domain.tech_article.repository;

import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.entity.TechArticle;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechArticleRepository extends JpaRepository<TechArticle, Long> {

	Optional<TechArticle> findBySourceAndRssGuid(ArticleSource source, String rssGuid);

	Page<TechArticle> findAllByOrderByUpdatedAtDesc(Pageable pageable);

	Page<TechArticle> findBySourceOrderByUpdatedAtDesc(ArticleSource source, Pageable pageable);
}
