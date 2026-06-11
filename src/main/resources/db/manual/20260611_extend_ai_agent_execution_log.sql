-- AI Agent 실행 로그에 LLM 호출 메타데이터를 추가한다.
-- 모델/프롬프트 버전/토큰 사용량을 저장해 비용과 품질을 추적한다.
ALTER TABLE ai_agent_execution_log
    ADD COLUMN error_code varchar(50) NULL,
    ADD COLUMN model varchar(100) NULL,
    ADD COLUMN prompt_version varchar(100) NULL,
    ADD COLUMN prompt_tokens int NULL,
    ADD COLUMN completion_tokens int NULL,
    ADD COLUMN total_tokens int NULL,
    ADD COLUMN reasoning_tokens int NULL,
    ADD COLUMN temperature double NULL;
