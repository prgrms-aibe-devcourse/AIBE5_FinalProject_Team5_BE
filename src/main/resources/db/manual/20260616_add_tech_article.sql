-- RSS 수집 기술 아티클 테이블
-- prod는 ddl-auto: validate이므로 애플리케이션 배포 전 먼저 적용해야 함
-- dev/local은 ddl-auto: update이므로 애플리케이션 기동 시 자동 생성됨

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `tech_article` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source` varchar(50) NOT NULL,
  `title` varchar(255) NOT NULL,
  `summary` text,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `author` varchar(100) DEFAULT NULL,
  `article_url` varchar(500) NOT NULL,
  `published_at` datetime NOT NULL,
  `rss_guid` varchar(500) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tech_article_source_rss_guid` (`source`, `rss_guid`),
  KEY `idx_tech_article_source_published_at` (`source`, `published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
