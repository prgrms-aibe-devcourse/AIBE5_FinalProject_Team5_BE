-- 인증 신청 처리 정보 및 DB 증빙 파일 저장 컬럼 추가
-- 용도: 사용자의 인증 신청 증빙 파일과 관리자 승인/반려 처리 이력을 저장
-- prod는 ddl-auto: validate이므로 애플리케이션 배포 전 먼저 적용해야 함
-- dev/local은 ddl-auto: update이므로 애플리케이션 기동 시 자동 반영됨

SET NAMES utf8mb4;

ALTER TABLE `verification`
  ADD COLUMN `evidence_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `evidence_content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `evidence_file_size` bigint DEFAULT NULL,
  ADD COLUMN `evidence_data` longblob DEFAULT NULL,
  ADD COLUMN `reject_reason` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `admin_memo` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `processed_by_id` bigint DEFAULT NULL,
  ADD COLUMN `processed_at` datetime(6) DEFAULT NULL;

ALTER TABLE `verification`
  ADD CONSTRAINT `fk_verification_processed_by`
    FOREIGN KEY (`processed_by_id`) REFERENCES `users` (`id`);

ALTER TABLE `verification`
  ADD UNIQUE KEY `uk_verification_user_course_session` (`user_id`, `course_session_id`);
