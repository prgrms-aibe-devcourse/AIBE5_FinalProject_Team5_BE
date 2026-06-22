# BootSignal 백엔드 배포 가이드

## 로컬 개발 환경 실행

1. `.env.example`을 복사해 `.env`로 생성 후 값 입력
2. 로컬 MySQL 및 Redis 실행
3. `./gradlew bootRun` 으로 실행 (기본 profile: `local`)

---

## 운영 환경 배포 (ECS Fargate + ECR)

### 필요한 환경 변수 (ECS Task Definition에 설정)

| 변수명 | 설명 |
|---|---|
| `DB_HOST` | RDS 엔드포인트 (예: `xxxx.rds.amazonaws.com`) |
| `DB_PORT` | RDS 포트 (기본값: `3306`) |
| `DB_NAME` | 데이터베이스 이름 (예: `bootsignal`) |
| `DB_USERNAME` | RDS 사용자명 |
| `DB_PASSWORD` | RDS 비밀번호 (AWS Secrets Manager 연동 권장) |
| `REDIS_HOST` | ElastiCache Redis 엔드포인트 |
| `REDIS_PORT` | Redis 포트 (기본값: `6379`) |
| `JWT_SECRET` | JWT 서명 키 (32자 이상, Secrets Manager 권장) |
| `CORS_ALLOWED_ORIGINS` | 허용할 프론트엔드 도메인 (예: `https://bootsignal.kr`) |
| `AWS_REGION` | AWS 리전 (예: `ap-northeast-2`) |
| `AWS_S3_BUCKET` | S3 버킷 이름 |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `KAKAO_CLIENT_ID` | Kakao OAuth Client ID |
| `OPENAI_API_KEY` | OpenAI API 키 |
| `HRD_API_AUTH_KEY` | 고용24 API 인증키 |

---

## Docker 이미지 빌드 및 ECR 푸시

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com

# 이미지 빌드
docker build -t bootsignal-backend .

# 태그
docker tag bootsignal-backend:latest \
  <AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/bootsignal-backend:latest

# 푸시
docker push <AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/bootsignal-backend:latest
```

---

## CodeBuild / CodePipeline

`buildspec.yml`은 Docker 이미지를 빌드하고 ECR에 push한 뒤, ECS 배포 액션이 사용할 `imagedefinitions.json`을 생성합니다.

CodeBuild 환경변수:

| 변수명 | 설명 |
|---|---|
| `AWS_DEFAULT_REGION` | AWS 리전 (예: `ap-northeast-2`) |
| `ECR_REGISTRY` | ECR 레지스트리 (예: `<AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com`) |
| `ECR_REPOSITORY_URI` | ECR repository 전체 URI (예: `<AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/bootsignal-backend`) |
| `CONTAINER_NAME` | ECS Task Definition의 container name. `buildspec.yml` 기본값은 `bootsignal-backend`이며, ECS container name과 정확히 일치해야 합니다. |

CodeBuild service role에는 ECR login/push 권한과 artifact 업로드 권한이 필요합니다.

---

## 로컬 MySQL → AWS RDS 데이터 이전

### 1. 로컬 DB dump 생성

```bash
mysqldump -u root -p bootsignal > bootsignal_dump.sql
```

기존 로컬 MySQL 데이터까지 RDS로 옮겨야 한다면 dump/import가 필요합니다. ECS에서 `ddl-auto=update`를 1회 실행하는 방법은 스키마 생성용 대안일 뿐이며, 데이터를 복사하지 않습니다.

### 2. RDS에 import

```bash
mysql -h <RDS_ENDPOINT> -P 3306 -u <RDS_USERNAME> -p bootsignal < bootsignal_dump.sql
```

### ⚠️ RDS Public Access가 비활성화된 경우

RDS가 프라이빗 서브넷에 있으면 로컬 PC에서 직접 접근이 불가합니다.
아래 방법 중 하나를 사용하세요:

**방법 A — AWS CloudShell**
1. AWS 콘솔 → CloudShell 접속
2. dump 파일 업로드 후 위의 import 명령어 실행

**방법 A-1 — RDS Public Access 임시 허용**
1. RDS Public access를 임시로 `Yes`로 변경
2. RDS Security Group inbound 3306을 내 공인 IP(`/32`)에만 임시 허용
3. 로컬 PC에서 import 명령어 실행
4. import 완료 후 Public access를 다시 `No`로 변경하고 임시 inbound 규칙 삭제

**방법 B — EC2 Bastion Host**
1. 동일 VPC 내 EC2 인스턴스에 dump 파일 전송
   ```bash
   scp -i <key.pem> bootsignal_dump.sql ec2-user@<EC2_PUBLIC_IP>:~/
   ```
2. EC2에서 RDS로 import
   ```bash
   mysql -h <RDS_ENDPOINT> -P 3306 -u <RDS_USERNAME> -p bootsignal < bootsignal_dump.sql
   ```

**방법 C — SSH 터널**
```bash
# 로컬에서 터널 설정
ssh -i <key.pem> -L 3307:<RDS_ENDPOINT>:3306 ec2-user@<EC2_PUBLIC_IP> -N &

# 터널을 통해 import
mysql -h 127.0.0.1 -P 3307 -u <RDS_USERNAME> -p bootsignal < bootsignal_dump.sql
```

---

## ddl-auto 주의사항

운영 환경(`prod` profile)은 `ddl-auto: validate`로 설정되어 있습니다.

- **validate**: 엔티티와 DB 스키마가 일치하지 않으면 **애플리케이션 시작 실패**
- 최초 배포 또는 스키마 변경 후에는 RDS에 DDL을 직접 실행해야 합니다

**초기 배포 시 임시 대안**: 첫 배포 때만 ECS 환경변수에 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`를 추가해 스키마를 자동 생성한 뒤, 이후 `validate`로 되돌리세요.

`SPRING_JPA_HIBERNATE_DDL_AUTO=update`는 초기 스키마 생성을 위한 임시 대안입니다. 로컬 DB의 기존 데이터까지 이전해야 한다면 반드시 `mysqldump`와 `mysql import`를 사용하세요.
