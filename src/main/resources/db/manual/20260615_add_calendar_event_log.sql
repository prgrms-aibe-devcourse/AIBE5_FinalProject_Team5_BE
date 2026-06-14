-- Google Calendar 이벤트 동기화 로그 테이블
-- 용도: 북마크 추가/삭제 시 Google Calendar 이벤트 생성/삭제 이력 저장
-- prod는 ddl-auto: validate이므로 애플리케이션 배포 전 먼저 적용해야 함
-- dev/local은 ddl-auto: update이므로 애플리케이션 기동 시 자동 생성됨

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `calendar_event_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `course_session_id` bigint NOT NULL,
  `google_event_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_start_at` datetime(6) NOT NULL,
  `event_end_at` datetime(6) NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calendar_event_log_user_course_session` (`user_id`, `course_session_id`),
  CONSTRAINT `fk_calendar_event_log_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_calendar_event_log_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`),
  CONSTRAINT `fk_calendar_event_log_course_session` FOREIGN KEY (`course_session_id`) REFERENCES `course_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
