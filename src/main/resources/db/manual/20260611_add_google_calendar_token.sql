-- Google Calendar OAuth 토큰 저장 테이블 (사용자당 1 row, UPDATE 방식)
-- 용도: 구글 캘린더 연동 access/refresh token 저장. revoked_at IS NULL 이면 활성 연동.
-- prod는 ddl-auto: validate이므로 애플리케이션 배포 전 먼저 적용해야 함
-- dev/local은 ddl-auto: update이므로 관련 api 실행 시 테이블 자동 생성됨

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `google_calendar_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `access_token_encrypted` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `refresh_token_encrypted` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scope` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `connected_at` datetime(6) NOT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_google_calendar_token_user_id` (`user_id`),
  CONSTRAINT `fk_google_calendar_token_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
