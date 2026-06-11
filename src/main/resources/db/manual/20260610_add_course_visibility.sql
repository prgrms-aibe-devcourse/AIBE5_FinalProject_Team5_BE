-- BootSignal: course 가시성(노출 상태)을 별도 테이블로 분리
-- 의미: course_visibility 행이 없으면 ACTIVE(노출). 관리자가 변경할 때만 행을 upsert 한다.
-- course 테이블에는 ALTER 를 적용하지 않는다 (status/status_reason 컬럼은 애초에 존재하지 않음).

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `course_visibility` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `status` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INACTIVE',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_visibility_course_id` (`course_id`),
  CONSTRAINT `fk_course_visibility_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
