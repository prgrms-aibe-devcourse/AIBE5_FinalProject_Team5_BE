-- AI Agent 공통 실행 로그 테이블
-- dev/prod는 ddl-auto: validate이므로 애플리케이션 배포 전 먼저 적용해야 한다.
CREATE TABLE ai_agent_execution_log (
    id bigint NOT NULL AUTO_INCREMENT,
    execution_id varchar(36) NOT NULL,
    agent_type varchar(50) NOT NULL,
    status varchar(20) NOT NULL,
    user_id bigint NULL,
    input_summary text NULL,
    input_hash varchar(64) NOT NULL,
    output_summary text NULL,
    error_message text NULL,
    retry_count int NOT NULL DEFAULT 0,
    started_at datetime(6) NOT NULL,
    finished_at datetime(6) NULL,
    elapsed_millis bigint NULL,
    created_at datetime(6) NULL,
    updated_at datetime(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_agent_execution_log_execution_id UNIQUE (execution_id)
);
