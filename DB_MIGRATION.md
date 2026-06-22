# DB Migration

## Current State

- Flyway is enabled in the application.
- Current schema baseline is `src/main/resources/db/migration/V1__baseline.sql`.
- The local Docker MySQL database has already been baselined in `flyway_schema_history` with version `1`.
- The local Docker MySQL instance currently runs 8.4, but the AWS RDS target is a lower MySQL version. Keep new DDL compatible with the lower RDS version, not with local 8.4-specific behavior.

## Rules

- Add all new schema changes under `src/main/resources/db/migration`.
- Use versioned files only: `V2__add_xxx.sql`, `V3__update_yyy.sql`, and so on.
- Do not add new schema changes under `src/main/resources/db/manual`.
- Keep JPA `ddl-auto=validate` for `local`, `dev`, and `prod`.

## Workflow

1. Change entity and application code.
2. Add the next Flyway migration file.
3. Apply it to the local DB.
4. Verify startup and run tests.
5. Promote the same migration to higher environments.

## Legacy SQL

`src/main/resources/db/manual` contains pre-Flyway SQL files kept only for reference.
