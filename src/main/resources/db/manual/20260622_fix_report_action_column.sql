-- report 테이블의 action 컬럼 크기 수정
-- 기존에 VARCHAR가 너무 작게 생성되어 INVALID_REASON(14자) 저장 시 truncate 오류 발생
-- VARCHAR(20)으로 확장하여 현재 enum 값(HIDE, INVALID_REASON)을 모두 수용

ALTER TABLE report
    MODIFY COLUMN action VARCHAR(20) NULL COMMENT 'ReportAction enum: HIDE | INVALID_REASON';
