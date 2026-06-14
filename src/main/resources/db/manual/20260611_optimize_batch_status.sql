-- 1. hrd_course_list_raw 테이블에 정제 여부 컬럼(is_refined) 추가
ALTER TABLE hrd_course_list_raw ADD COLUMN is_refined TINYINT NOT NULL DEFAULT 0;

-- 2. course_session 테이블에 크롤링 완료 일시 컬럼(crawled_at) 추가
ALTER TABLE course_session ADD COLUMN crawled_at datetime(6) DEFAULT NULL;


