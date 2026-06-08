-- 소셜 로그인 계정 식별과 비밀번호 없는 계정 저장을 위한 수동 DB 반영 SQL
-- dev/prod는 ddl-auto: validate이므로 애플리케이션 배포 전 먼저 적용해야 한다.
ALTER TABLE users
    MODIFY COLUMN password_hash varchar(255) NULL;

ALTER TABLE users
    ADD COLUMN provider_user_id varchar(255) NULL AFTER provider;

CREATE UNIQUE INDEX uk_users_provider_provider_user_id
    ON users (provider, provider_user_id);
