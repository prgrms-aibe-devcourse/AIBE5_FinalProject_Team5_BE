-- 북마크(스크랩) 테이블
-- 용도: 사용자가 관심 과정 회차를 저장. (user_id, course_session_id) 유니크
-- prod는 ddl-auto: validate이므로 애플리케이션 배포 전 먼저 적용해야 함
-- dev/local은 ddl-auto: update이므로 애플리케이션 기동 시 자동 생성됨

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_session_id` bigint NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bookmark_user_course_session` (`user_id`, `course_session_id`),
  CONSTRAINT `fk_bookmark_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_bookmark_course_session` FOREIGN KEY (`course_session_id`) REFERENCES `course_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
