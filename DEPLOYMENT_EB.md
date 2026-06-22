# BootSignal Backend Elastic Beanstalk Deployment

Use this path when ECS is not available in the AWS account.

## Architecture

```text
GitHub backend repository
  -> GitHub Actions or CodePipeline / CodeBuild
  -> Elastic Beanstalk Java 21 environment
  -> RDS MySQL / S3 / Redis
```

## Repository Files

Elastic Beanstalk deployment uses:

- `buildspec.yml`: builds the Spring Boot executable jar for CodeBuild.
- `Procfile`: tells Elastic Beanstalk how to run the app.
- `.github/workflows/deploy-eb.yml`: deploys from GitHub Actions to Elastic Beanstalk.
- `application-prod.yml`: reads production settings from environment variables.

The ECS/ECR buildspec is preserved as `buildspec-ecs.yml` for future use.

## GitHub Actions Deployment

If AWS CodeConnections or CodePipeline source integration is blocked by account policy, use GitHub Actions.

Workflow file:

```text
.github/workflows/deploy-eb.yml
```

Trigger:

- push to `main`
- manual `workflow_dispatch`

Required GitHub repository secrets:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

The workflow:

1. Runs Gradle tests and builds the Spring Boot jar.
2. Copies the built jar to `application.jar`.
3. Creates `deployment.zip` with:
   - `application.jar`
   - `Procfile`
4. Uploads the zip to the Elastic Beanstalk artifact S3 bucket.
5. Creates a new Elastic Beanstalk application version.
6. Updates the `team5-bootsignal-prod` environment.

Current default values inside the workflow:

| Item | Value |
| --- | --- |
| AWS region | `ap-northeast-2` |
| EB application | `team5-bootsignal` |
| EB environment | `team5-bootsignal-prod` |
| EB artifact bucket | `elasticbeanstalk-ap-northeast-2-495264909330` |

## Elastic Beanstalk App

Recommended values:

| Item | Value |
| --- | --- |
| Application name | `team5-bootsignal` |
| Environment name | `team5-bootsignal-prod` |
| Platform | Java |
| Platform branch | Corretto 21 / Java 21 |
| Environment type | Single instance for first deploy |
| Database | AWS RDS MySQL below 8.4 |

The `Procfile` runs the app on port `5000`, which is the standard port used by Elastic Beanstalk Java SE reverse proxy configuration.

## Environment Variables

Set these in the Elastic Beanstalk environment configuration.

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST=<RDS_ENDPOINT>
DB_PORT=3306
DB_NAME=bootsignal
DB_USERNAME=<RDS_USERNAME>
DB_PASSWORD=<RDS_PASSWORD>
JWT_SECRET=<strong-secret>
REDIS_HOST=<redis-host>
REDIS_PORT=6379
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=<prod-s3-bucket>
```

If the RDS schema is empty on the first deployment, temporarily add:

```text
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

After tables are created, remove it or set it back to:

```text
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

Do not hardcode passwords, RDS endpoints, AWS keys, or JWT secrets in source code.

## RDS Security Group

Because RDS public access is disabled, allow inbound MySQL traffic from the Elastic Beanstalk EC2 instance security group.

| Type | Port | Source |
| --- | --- | --- |
| MySQL/Aurora | 3306 | Elastic Beanstalk EC2 security group |

Do not open port `3306` to `0.0.0.0/0`.

## CodeBuild

Use the default `buildspec.yml`.

It creates this deployment artifact:

```text
application.jar
Procfile
```

Docker privileged mode is not required for this Elastic Beanstalk path.

## CodePipeline

Create a pipeline with these stages:

1. Source: GitHub backend repository.
2. Build: CodeBuild project using `buildspec.yml`.
3. Deploy: Elastic Beanstalk application/environment.

On every push to the selected branch, CodePipeline will build the jar and deploy it to Elastic Beanstalk.

## Data Migration

If existing local MySQL data must be moved to RDS, use dump/import.

```bash
mysqldump -u root -p bootsignal > bootsignal_dump.sql
mysql -h <RDS_ENDPOINT> -P 3306 -u <RDS_USERNAME> -p bootsignal < bootsignal_dump.sql
```

Since RDS public access is disabled, run the import from inside the same VPC, such as an EC2 bastion host, or temporarily enable public access with only your IP allowed and disable it again immediately after import.
