# Legacy Manual SQL

This directory contains pre-Flyway manual schema change scripts kept only for reference.

Rules:

- Do not add new schema changes here.
- Add all new schema changes under `src/main/resources/db/migration`.
- Use these files only to understand older schema history when needed.

Current Flyway baseline:

- `src/main/resources/db/migration/V1__baseline.sql`
