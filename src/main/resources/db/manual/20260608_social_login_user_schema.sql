-- 소셜 로그인 계정 식별과 비밀번호 없는 계정 저장을 위한 수동 DB 반영 SQL
-- ddl-auto: update 설정에서는 애플리케이션 기동 시 자동 반영된다.
-- 수동 반영이 필요한 환경에서만 참고용으로 사용한다.
ALTER TABLE users
    MODIFY COLUMN password_hash varchar(255) NULL;

ALTER TABLE users
    ADD COLUMN provider_user_id varchar(255) NULL AFTER provider;

CREATE UNIQUE INDEX uk_users_provider_provider_user_id
    ON users (provider, provider_user_id);
