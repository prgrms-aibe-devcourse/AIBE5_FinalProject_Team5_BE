CREATE TABLE `review_summary_cache` (
  `id`                 bigint        NOT NULL AUTO_INCREMENT,
  `created_at`         datetime(6)   DEFAULT NULL,
  `updated_at`         datetime(6)   DEFAULT NULL,
  `course_id`          bigint        NOT NULL,
  `execution_id`       varchar(36)   COLLATE utf8mb4_unicode_ci NOT NULL,
  `review_count`       int           NOT NULL,
  `latest_crawled_at`  datetime(6)   NOT NULL,
  `average_rating`     decimal(3,2)  DEFAULT NULL,
  `course_title`       varchar(500)  COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary`            text          COLLATE utf8mb4_unicode_ci NOT NULL,
  `strengths`          text          COLLATE utf8mb4_unicode_ci,
  `weaknesses`         text          COLLATE utf8mb4_unicode_ci,
  `recommended_for`    text          COLLATE utf8mb4_unicode_ci,
  `keywords`           text          COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_summary_cache_course_id` (`course_id`),
  CONSTRAINT `fk_review_summary_cache_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
