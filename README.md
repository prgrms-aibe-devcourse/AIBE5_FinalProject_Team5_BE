# BootSignal Backend

BootSignal은 인증된 수강생의 후기와 조건별 통계를 기반으로, 예비 수강생이 부트캠프를 데이터로 판단할 수 있도록 돕는 서비스입니다.

이 저장소는 BootSignal의 백엔드 애플리케이션입니다.

## 전체 기술 스택

### 프론트엔드

- Next.js
- React
- Tailwind CSS
- SSR 기반 과정 상세 페이지
- 백엔드 REST API 연동

프론트엔드 저장소: [prgrms-aibe-devcourse/AIBE5_FinalProject_Team5_FE](https://github.com/prgrms-aibe-devcourse/AIBE5_FinalProject_Team5_FE)

### 백엔드

- Java 21
- Gradle Wrapper
- Spring Boot 3.5.14
- Spring Web
- Spring Data JPA
- Spring Security
- OAuth2 Client
- JWT(JJWT)
- MySQL
- H2
- Redis
- AWS S3 SDK
- OpenAI API 연동 준비
- Docker, Docker Compose
- GitHub Actions CI

## 연동 설계

프론트엔드는 Next.js를 사용하고, 백엔드는 Spring Boot REST API 서버로 분리합니다.

- Next.js는 사용자 화면, SSR, SEO 최적화를 담당합니다.
- Spring Boot는 인증, 권한, 데이터 처리, 파일 저장, AI 요약 연동을 담당합니다.
- 과정 목록/상세 페이지는 검색 유입이 중요하므로 Next.js SSR 또는 Server Component에서 백엔드 API를 호출하는 구조를 기준으로 합니다.
- 브라우저에서 직접 호출하는 API는 CORS 허용 origin을 통해 제어합니다.

백엔드 CORS 설정은 `CORS_ALLOWED_ORIGINS` 환경 변수로 관리합니다.

## 프로파일

- `local`: 기본 실행 프로파일입니다. H2 인메모리 DB와 로컬 Redis 설정을 사용합니다.
- `dev`: Docker Compose로 실행한 MySQL, Redis를 사용합니다.
- `prod`: 배포 환경용 프로파일입니다. DB, Redis, 외부 서비스 값은 환경 변수로 주입합니다.
- `test`: 테스트 실행용 H2 인메모리 DB를 사용합니다.

## 로컬 실행

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

기본값으로 `local` 프로파일이 적용됩니다.

## 개발 인프라 실행

MySQL과 Redis를 Docker로 실행합니다.

```bash
docker compose up -d mysql redis
```

`dev` 프로파일로 애플리케이션을 실행합니다.

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

## 테스트

Windows:

```powershell
.\gradlew.bat clean test
```

macOS/Linux:

```bash
./gradlew clean test
```

## 빌드

Windows:

```powershell
.\gradlew.bat clean build
```

macOS/Linux:

```bash
./gradlew clean build
```

## 환경 변수

환경 변수 예시는 [.env.example](.env.example)을 기준으로 확인합니다. 실제 `.env` 파일과 비밀 값은 Git에 커밋하지 않습니다.

주요 환경 변수:

- `SPRING_PROFILES_ACTIVE`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `AWS_REGION`
- `AWS_S3_BUCKET`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`

## 현재 초기 셋팅 범위

현재 저장소에는 기술스택 기반의 백엔드 실행 환경만 구성되어 있습니다. ERD, API 스키마, 도메인 엔티티, 비즈니스 API 구현은 아직 반영하지 않았습니다.
