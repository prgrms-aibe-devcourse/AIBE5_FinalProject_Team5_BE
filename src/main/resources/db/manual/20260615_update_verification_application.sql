-- 인증 신청 처리 정보 및 DB 임시 파일 저장 컬럼 추가
-- 용도: 사용자의 인증 신청 제출 자료와 관리자 승인/반려 처리 이력을 저장
-- prod는 ddl-auto: validate이므로 애플리케이션 배포 전 먼저 적용해야 함
-- dev/local은 ddl-auto: update로 애플리케이션 기동 시 자동 반영 가능
SET NAMES utf8mb4;

ALTER TABLE `verification`
  ADD COLUMN `job_training_history_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `job_training_history_content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `job_training_history_file_size` bigint DEFAULT NULL,
  ADD COLUMN `job_training_history_data` longblob DEFAULT NULL,
  ADD COLUMN `online_course_application_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `online_course_application_content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `online_course_application_file_size` bigint DEFAULT NULL,
  ADD COLUMN `online_course_application_data` longblob DEFAULT NULL,
  ADD COLUMN `reject_reason` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `admin_memo` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `processed_by_id` bigint DEFAULT NULL,
  ADD COLUMN `processed_at` datetime(6) DEFAULT NULL;

ALTER TABLE `verification`
  ADD CONSTRAINT `fk_verification_processed_by`
    FOREIGN KEY (`processed_by_id`) REFERENCES `users` (`id`);

ALTER TABLE `verification`
  ADD UNIQUE KEY `uk_verification_user_course_session` (`user_id`, `course_session_id`);
