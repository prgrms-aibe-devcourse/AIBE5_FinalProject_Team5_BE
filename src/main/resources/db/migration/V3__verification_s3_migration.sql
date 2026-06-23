-- 수강 인증 자료를 DB BLOB에서 S3로 이전하기 위한 스키마 변경
-- LONGBLOB 컬럼 제거, S3 오브젝트 키 컬럼 추가

ALTER TABLE verification
    ADD COLUMN job_training_history_s3_key VARCHAR(500) AFTER job_training_history_file_size,
    ADD COLUMN online_course_application_s3_key VARCHAR(500) AFTER online_course_application_file_size,
    DROP COLUMN job_training_history_data,
    DROP COLUMN online_course_application_data;
