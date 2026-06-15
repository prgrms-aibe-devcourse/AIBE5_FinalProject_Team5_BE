-- BootSignal main 전체 DB 스키마
-- 기준: origin/main 6168d1238bc35bf274b1f20e5cfaefbd6439c25d
-- 용도: 빈 MySQL DB에 서비스 실행에 필요한 모든 테이블을 생성한다.
-- 주의: 기존 테이블 구조가 다른 환경에서는 별도 마이그레이션을 적용해야 한다.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `users` (
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_image_url` text COLLATE utf8mb4_unicode_ci,
  `provider_user_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider` enum('GOOGLE','KAKAO','LOCAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('ADMIN','USER') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`),
  UNIQUE KEY `uk_users_nickname` (`nickname`),
  UNIQUE KEY `uk_users_provider_provider_user_id` (`provider`, `provider_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `institution` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `profile_image_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `homepage_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `inst_cd` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `institution_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `introduction` text COLLATE utf8mb4_unicode_ci,
  `manager_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manager_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manager_tel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqeun15b7gi4kxna06y86hjvyt` (`inst_cd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course` (
  `course_man` decimal(38,2) DEFAULT NULL,
  `real_man` decimal(38,2) DEFAULT NULL,
  `self_payment_amount` decimal(38,2) DEFAULT NULL,
  `stdg_scor` decimal(38,2) DEFAULT NULL,
  `total_training_days` int DEFAULT NULL,
  `total_training_hours` int DEFAULT NULL,
  `crawled_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `institution_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `sub_title_link` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title_link` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ncs_cd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ncs_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ncs_yn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sub_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `training_goal` text COLLATE utf8mb4_unicode_ci,
  `training_target_requirements` text COLLATE utf8mb4_unicode_ci,
  `trng_area_cd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trpr_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcyqlmqpc3brn02qoiihb3gs6r` (`trpr_id`),
  KEY `FKeia6xjuespxibcj584q9k69b1` (`institution_id`),
  CONSTRAINT `FKeia6xjuespxibcj584q9k69b1` FOREIGN KEY (`institution_id`) REFERENCES `institution` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_session` (
  `confirmed_trainee_count` int DEFAULT NULL,
  `employment_rate` decimal(38,2) DEFAULT NULL,
  `fini_cnt` int DEFAULT NULL,
  `recruitment_count` int DEFAULT NULL,
  `reg_course_man` int DEFAULT NULL,
  `selected_trainee_count` int DEFAULT NULL,
  `tot_par_mks` int DEFAULT NULL,
  `tra_end_date` date DEFAULT NULL,
  `tra_start_date` date DEFAULT NULL,
  `trpr_degr` int DEFAULT NULL,
  `yard_man` int DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `ei_empl_rate3` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ei_empl_rate6` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trpr_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `wkend_se` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK45uvohj74w2l6s5ua0c6ypohw` (`trpr_id`, `trpr_degr`),
  KEY `FKqp6t0vjo98oghqcp26ql0m22b` (`course_id`),
  CONSTRAINT `FKqp6t0vjo98oghqcp26ql0m22b` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `verification` (
  `course_id` bigint NOT NULL,
  `course_session_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `processed_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `processed_by_id` bigint DEFAULT NULL,
  `evidence_file_size` bigint DEFAULT NULL,
  `admin_memo` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evidence_data` longblob DEFAULT NULL,
  `reject_reason` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evidence_content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evidence_file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_verification_user_course_session` (`user_id`, `course_session_id`),
  KEY `FKl4xbomskkaxodid0eue6q6rup` (`course_id`),
  KEY `FKjyf2sr4l4jg5nw0dbn0ubdw4k` (`course_session_id`),
  KEY `FK7ntgdvdvok1jx29t3uooau08j` (`user_id`),
  KEY `fk_verification_processed_by` (`processed_by_id`),
  CONSTRAINT `FK7ntgdvdvok1jx29t3uooau08j` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKjyf2sr4l4jg5nw0dbn0ubdw4k` FOREIGN KEY (`course_session_id`) REFERENCES `course_session` (`id`),
  CONSTRAINT `FKl4xbomskkaxodid0eue6q6rup` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`),
  CONSTRAINT `fk_verification_processed_by` FOREIGN KEY (`processed_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post` (
  `is_valid` bit(1) NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `post_type` enum('ARTICLE','BOARD','PROJECT_RECRUIT','QNA') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKe7p5x3rqf74eb00ynw9x85l5r` (`course_id`),
  KEY `FK7ky67sgi7k0ayf22652f7763r` (`user_id`),
  CONSTRAINT `FK7ky67sgi7k0ayf22652f7763r` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKe7p5x3rqf74eb00ynw9x85l5r` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `review` (
  `rating` int NOT NULL,
  `course_id` bigint NOT NULL,
  `course_session_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `review_type` enum('GENERAL','VERIFIED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5b1l439c4re119r7e0wuljaui` (`user_id`, `course_session_id`),
  KEY `FKprox8elgnr8u5wrq1983degk` (`course_id`),
  KEY `FK2x2pd5y333b7ch3rbhvpnlmpr` (`course_session_id`),
  CONSTRAINT `FK2x2pd5y333b7ch3rbhvpnlmpr` FOREIGN KEY (`course_session_id`) REFERENCES `course_session` (`id`),
  CONSTRAINT `FK6cpw2nlklblpvc7hyt7ko6v3e` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKprox8elgnr8u5wrq1983degk` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `hrd_course_list_raw` (
  `trpr_degr` int NOT NULL,
  `fetched_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sub_title_link` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title_link` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `course_man` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `inst_cd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ncs_cd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `real_man` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reg_course_man` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stdg_scor` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sub_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tra_end_date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tra_start_date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `train_target_cd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trainst_cstmr_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trng_area_cd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trpr_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `wkend_se` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `yard_man` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgeaiyi0nrntuonkkjb2lnnq3r` (`trpr_id`, `trpr_degr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `hrd_course_detail_raw` (
  `trpr_degr` int NOT NULL,
  `fetched_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `hp_addr` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ncs_nm` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ncs_yn` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tgcr_gnrl_trne_owep_allt` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tr_dcnt` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trpr_chap` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trpr_chap_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trpr_chap_tel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trpr_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trtm` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp8nucugkwh2gu3h3ysr2kapbk` (`trpr_id`, `trpr_degr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `hrd_training_schedule_raw` (
  `trpr_degr` int NOT NULL,
  `fetched_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ei_empl_rate3` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ei_empl_rate6` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fini_cnt` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tot_par_mks` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trpr_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKptq282fuxdr356l7ool3w2qid` (`trpr_id`, `trpr_degr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_STEP_EXECUTION_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_INSTANCE` (
  `JOB_INSTANCE_ID` bigint NOT NULL,
  `VERSION` bigint DEFAULT NULL,
  `JOB_NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_KEY` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`JOB_INSTANCE_ID`),
  UNIQUE KEY `JOB_INST_UN` (`JOB_NAME`, `JOB_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `VERSION` bigint DEFAULT NULL,
  `JOB_INSTANCE_ID` bigint NOT NULL,
  `CREATE_TIME` datetime(6) NOT NULL,
  `START_TIME` datetime(6) DEFAULT NULL,
  `END_TIME` datetime(6) DEFAULT NULL,
  `STATUS` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_CODE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UPDATED` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  KEY `JOB_INST_EXEC_FK` (`JOB_INSTANCE_ID`),
  CONSTRAINT `JOB_INST_EXEC_FK` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `BATCH_JOB_INSTANCE` (`JOB_INSTANCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION_PARAMS` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `PARAMETER_NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `PARAMETER_TYPE` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `PARAMETER_VALUE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IDENTIFYING` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  KEY `JOB_EXEC_PARAMS_FK` (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_PARAMS_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION_CONTEXT` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `SHORT_CONTEXT` varchar(2500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SERIALIZED_CONTEXT` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_CTX_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_STEP_EXECUTION` (
  `STEP_EXECUTION_ID` bigint NOT NULL,
  `VERSION` bigint NOT NULL,
  `STEP_NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `CREATE_TIME` datetime(6) NOT NULL,
  `START_TIME` datetime(6) DEFAULT NULL,
  `END_TIME` datetime(6) DEFAULT NULL,
  `STATUS` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COMMIT_COUNT` bigint DEFAULT NULL,
  `READ_COUNT` bigint DEFAULT NULL,
  `FILTER_COUNT` bigint DEFAULT NULL,
  `WRITE_COUNT` bigint DEFAULT NULL,
  `READ_SKIP_COUNT` bigint DEFAULT NULL,
  `WRITE_SKIP_COUNT` bigint DEFAULT NULL,
  `PROCESS_SKIP_COUNT` bigint DEFAULT NULL,
  `ROLLBACK_COUNT` bigint DEFAULT NULL,
  `EXIT_CODE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UPDATED` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  KEY `JOB_EXEC_STEP_FK` (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_STEP_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_STEP_EXECUTION_CONTEXT` (
  `STEP_EXECUTION_ID` bigint NOT NULL,
  `SHORT_CONTEXT` varchar(2500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SERIALIZED_CONTEXT` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  CONSTRAINT `STEP_EXEC_CTX_FK` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `BATCH_STEP_EXECUTION` (`STEP_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Spring Batch 시퀀스 테이블 초기값
INSERT INTO `BATCH_JOB_SEQ` (`ID`, `UNIQUE_KEY`)
VALUES (0, '0')
ON DUPLICATE KEY UPDATE `ID` = `ID`;

INSERT INTO `BATCH_JOB_EXECUTION_SEQ` (`ID`, `UNIQUE_KEY`)
VALUES (0, '0')
ON DUPLICATE KEY UPDATE `ID` = `ID`;

INSERT INTO `BATCH_STEP_EXECUTION_SEQ` (`ID`, `UNIQUE_KEY`)
VALUES (0, '0')
ON DUPLICATE KEY UPDATE `ID` = `ID`;

SET FOREIGN_KEY_CHECKS = 1;
