# 배치 환경 및 데이터 수집/정제 가이드

이 문서는 BootSignal 프로젝트의 배치 시스템과 각 배치 작업이 수집·정제하는 데이터 흐름, 참고 및 갱신하는 DB 컬럼 등의 명세를 기술합니다.

---

## 1. 배치 스케줄러 개요
AWS Elastic Beanstalk (Single Instance) 환경을 고려하여 구축되었습니다. 스케줄러는 `SCHEDULER_ENABLED=true` 환경변수로 명시적 활성화해야 작동하며, 비동기 `JobLauncher` 및 `AtomicBoolean` 제어 플래그를 통해 동시 실행 방지와 안전한 구동을 보장합니다.

### 📅 변경된 배치 스케줄 (초기 설정 완화)
서버 부하와 외부 OpenAPI 호출 제한을 고려하여 기존 매일/매주 일요일 실행 주기를 **주 1~2회**로 널널하게 조정하였습니다.

```
[월요일]
  ├── 02:00 : hrdDataCollectJob (오픈 API 수집)
  ├── 03:00 : hrdDataRefineJob (Raw -> 서비스 DB 정제)
  └── 09:00 : TechArticle RSS 수집 (RSS 아티클 수집)

[수요일]
  ├── 02:00 : hrdWebCrawlJob (HTML 상세 페이지 크롤링 보강)
  └── 04:00 : reviewCrawlJob (수강후기 크롤링 - maxPages=10 상한)
```

---

## 2. 배치 Job별 상세 설명 및 참고 컬럼

### ① `hrdDataCollectJob` (고용24 API 데이터 수집)
고용24 오픈 API를 통해 훈련과정 목록, 상세 정보, 일정/통계 정보를 수집하여 `HrdCourseListRaw`, `HrdCourseDetailRaw`, `HrdTrainingScheduleRaw` 테이블에 적재합니다.

* **실행 주기**: 매주 월요일 02:00 (`0 0 2 * * MON`)
* **Step 구성**:
  1. **`collectListStep`**:
     - **API**: 고용24 훈련과정 목록 조회 (`hrdApiClient.fetchCourseList`)
     - **파라미터**: `startDate` (오늘), `endDate` (오늘 + 3개월)
     - **필터링 조건**: `trainTargetCd` (훈련대상코드)가 `"C0104"` (K-디지털 트레이닝)가 아닌 과정은 수집에서 제외 (`courseListProcessor`)
     - **대상 테이블**: `HRD_COURSE_LIST_RAW` (Upsert: `trprId` + `trprDegr`)
  2. **`collectDetailStep`**:
     - **API**: 고용24 훈련과정 상세 조회 (`hrdApiClient.fetchCourseDetail`)
     - **대상 선정**: `HRD_COURSE_LIST_RAW` 테이블에는 존재하나 `HRD_COURSE_DETAIL_RAW` 테이블에는 없는 과정 조회 (N+1 방지 `LEFT JOIN` 쿼리)
     - **대상 테이블**: `HRD_COURSE_DETAIL_RAW` (Upsert: `trprId` + `trprDegr`)
  3. **`collectScheduleStep`**:
     - **API**: 고용24 훈련과정 일정/통계 조회 (`hrdApiClient.fetchTrainingSchedule`)
     - **대상 선정**: `HRD_COURSE_LIST_RAW` 테이블에는 존재하나 `HRD_TRAINING_SCHEDULE_RAW` 테이블에는 없는 과정 조회 (N+1 방지 `LEFT JOIN` 쿼리)
     - **대상 테이블**: `HRD_TRAINING_SCHEDULE_RAW` (Upsert: `trprId` + `trprDegr`)

---

### ② `hrdDataRefineJob` (Raw 테이블 데이터 정제)
수집된 Raw 테이블 데이터를 조합하여 서비스 비즈니스 엔티티(`Institution`, `Course`, `CourseSession`)로 변환 및 적재합니다.

* **실행 주기**: 매주 월요일 03:00 (`0 0 3 * * MON` - 수집 1시간 후 실행)
* **정제 대상**: `HRD_COURSE_LIST_RAW` 테이블 중 `isRefined = false` 인 데이터

#### 1. `refineInstitutionStep` (훈련기관 정보 정제)
* **대상 식별**: `instCd` (기관코드) 기준 중복 확인 (Upsert)
* **참고 및 갱신 컬럼 매핑**:
  | 서비스 엔티티 (`INSTITUTION`) | 소스 Raw 테이블 및 컬럼 | 설명 |
  | :--- | :--- | :--- |
  | `instCd` | `HRD_COURSE_LIST_RAW.instCd` | 기관 코드 (식별자) |
  | `institutionName` | `HRD_COURSE_LIST_RAW.subTitle` | 훈련기관명 |
  | `address` | `HRD_COURSE_LIST_RAW.address` | 훈련기관 주소 |
  | `homepageUrl` | `HRD_COURSE_DETAIL_RAW.hpAddr` | 기관 홈페이지 URL (선택) |
  | `managerName` | `HRD_COURSE_DETAIL_RAW.trprChap` | 과정 담당자명 (선택) |
  | `managerTel` | `HRD_COURSE_DETAIL_RAW.trprChapTel` | 담당자 전화번호 (선택) |
  | `managerEmail` | `HRD_COURSE_DETAIL_RAW.trprChapEmail` | 담당자 이메일 (선택) |

#### 2. `refineCourseStep` (훈련과정 정보 정제)
* **대상 식별**: `trprId` (훈련과정 ID) 기준 중복 확인 (Upsert)
* **참고 및 갱신 컬럼 매핑**:
  | 서비스 엔티티 (`COURSE`) | 소스 Raw 테이블 및 컬럼 | 설명 |
  | :--- | :--- | :--- |
  | `trprId` | `HRD_COURSE_LIST_RAW.trprId` | 과정 ID (식별자) |
  | `title` | `HRD_COURSE_LIST_RAW.title` | 과정명 |
  | `subTitle` | `HRD_COURSE_LIST_RAW.subTitle` | 기관명 |
  | `subTitleLink` | `HRD_COURSE_LIST_RAW.subTitleLink` | 기관 정보 링크 |
  | `ncsCd` | `HRD_COURSE_LIST_RAW.ncsCd` | NCS 분류 코드 |
  | `ncsName` | `HRD_COURSE_DETAIL_RAW.ncsNm` | NCS 분류명 (선택) |
  | `ncsYn` | `HRD_COURSE_DETAIL_RAW.ncsYn` | NCS 여부 (Y/N) (선택) |
  | `stdgScor` | `HRD_COURSE_LIST_RAW.stdgScor` | 수강생 만족도 점수 (`BigDecimal` 변환) |
  | `trngAreaCd` | `HRD_COURSE_LIST_RAW.trngAreaCd` | 훈련 지역 코드 |
  | `institution_id` | `INSTITUTION.id` | 정제 완료된 기관 엔티티 외래키 매핑 |

#### 3. `refineCourseSessionStep` (훈련과정 회차 정보 정제)
* **대상 식별**: `trprId` + `resolvedDegr` (실제 회차) 기준 중복 확인 (Upsert)
* **회차 계산 (`resolvedDegr`)**: `titleLink`에서 `tracseTme` 파라미터(예: `...tracseTme=7...` -> `7`)를 파싱하여 구하고, 파싱 실패 시 기본 `trprDegr`를 fallback으로 사용
* **참고 및 갱신 컬럼 매핑**:
  | 서비스 엔티티 (`COURSE_SESSION`) | 소스 Raw 테이블 및 컬럼 | 설명 |
  | :--- | :--- | :--- |
  | `trprId` | `HRD_COURSE_LIST_RAW.trprId` | 과정 ID |
  | `trprDegr` | `resolvedDegr` | 계산된 실제 과정 회차 |
  | `traStartDate` | `HRD_COURSE_LIST_RAW.traStartDate` | 훈련 시작일 (`LocalDate` 변환) |
  | `traEndDate` | `HRD_COURSE_LIST_RAW.traEndDate` | 훈련 종료일 (`LocalDate` 변환) |
  | `yardMan` | `HRD_COURSE_LIST_RAW.yardMan` | 정원 (`Integer` 변환) |
  | `regCourseMan` | `HRD_COURSE_LIST_RAW.regCourseMan` | 모집 인원 (`Integer` 변환) |
  | `totParMks` | `HRD_TRAINING_SCHEDULE_RAW.totParMks` | 총 참여자수 (`Integer` 변환, 선택) |
  | `finiCnt` | `HRD_TRAINING_SCHEDULE_RAW.finiCnt` | 수료 인원 (`Integer` 변환, 선택) |
  | `eiEmplRate3` | `HRD_TRAINING_SCHEDULE_RAW.eiEmplRate3` | 3개월 취업률 (선택) |
  | `eiEmplRate6` | `HRD_TRAINING_SCHEDULE_RAW.eiEmplRate6` | 6개월 취업률 (선택) |
  | `wkendSe` | `HRD_COURSE_LIST_RAW.wkendSe` | 주말 구분 코드 (S: 토, SUN: 일, W: 평일 등) |
  | `titleLink` | `HRD_COURSE_LIST_RAW.titleLink` | 과정 상세 URL |
  | `courseMan` | `HRD_COURSE_LIST_RAW.courseMan` | 수강 신청 인원 (`Integer` 변환) |
  | `selfPaymentAmount` | `HRD_COURSE_DETAIL_RAW.tgcrGnrlTrneOwepAllt` | 일반훈련생 기준 자부담금액 (`Integer` 변환, 선택) |
  | `totalTrainingDays` | `HRD_COURSE_DETAIL_RAW.trDcnt` | 총 훈련 일수 (`Integer` 변환, 선택) |
  | `totalTrainingHours` | `HRD_COURSE_DETAIL_RAW.trtm` | 총 훈련 시간 (`Integer` 변환, 선택) |
  | `course_id` | `COURSE.id` | 정제 완료된 과정 엔티티 외래키 매핑 |
* **마크 처리**: 정제가 완료되면 `HRD_COURSE_LIST_RAW.isRefined`를 `true`로 업데이트하여 다음 정제 대상에서 제외합니다.

---

### ③ `hrdWebCrawlJob` (훈련과정 상세 페이지 HTML 크롤링 보강)
고용24 상세 웹페이지(`titleLink`)를 직접 크롤링하여 API 데이터에 누락되거나 부족한 추가 정보를 보완합니다.

* **실행 주기**: 매주 수요일 02:00 (`0 0 2 * * WED`)
* **크롤링 대상**: `COURSE_SESSION` 중 `titleLink`가 비어있지 않고, `crawledAt`이 `null`인 데이터
* **크롤링 속도 제한**: `delayMillis` (기본 1500ms) 만큼 대기하여 고용24 서버에 과도한 부하 차단
* **참고 및 갱신 컬럼 매핑**:
  - **`COURSE_SESSION`** 업데이트:
    - `selectedTraineeCount`: 선발인원
    - `recruitmentCount`: 모집인원
    - `confirmedTraineeCount`: 확정인원
    - `employmentRate`: 취업률
    - `crawledAt`: 크롤링 일시 (`Instant.now()`)
  - **`COURSE`** 업데이트:
    - `trainingTargetRequirements`: 훈련 대상 요건
    - `trainingGoal`: 훈련 목표
    - `crawledAt`: 크롤링 일시
  - **`INSTITUTION`** 업데이트:
    - `institutionProfileImageUrl`: 기관 프로필 이미지 URL
    - `institutionIntroduction`: 기관 소개글

---

### ④ `reviewCrawlJob` (수강후기 크롤링)
고용24 과정 상세의 수강후기 페이지를 순회하며 개별 리뷰를 수집 및 적재합니다.

* **실행 주기**: 매주 수요일 04:00 (`0 0 4 * * WED`)
* **크롤링 대상**: `COURSE` 중 `reviewCrawledAt`이 `null`인 데이터
* **크롤링 상한**: 과정당 최대 수집 페이지 수 `maxPages` (기본값 10, 즉 최대 100개 리뷰)로 제한하여 리소스 보호
* **참고 및 갱신 컬럼 매핑**:
  - **`CRAWLED_REVIEW`** 테이블 (신규 저장):
    - `course_id`: 대상 과정 ID
    - `source`: 수집 소스 (`WORK24` 고정)
    - `externalReviewId`: 고용24 리뷰 고유 ID (중복 확인을 위해 `findExternalReviewIdsByCourseId` 벌크 조회 활용)
    - `reviewerNickname`: 리뷰 작성자 닉네임
    - `rating`: 평점
    - `content`: 후기 텍스트 내용
    - `reviewedAt`: 후기 등록일
    - `crawledAt`: 수집 일시
  - **`COURSE`** 업데이트:
    - `reviewCrawledAt`: 크롤링 완료 일시 (`Instant.now()`)로 채워져 다음 크롤링 주기에서 제외 처리

---

### ⑤ `TechArticle RSS 수집`
등록된 IT 기술 블로그 RSS 피드들을 읽어, 개발 관련 핵심 키워드가 포함된 아티클만 필터링하여 수집합니다.

* **실행 주기**: 매주 월요일 09:00 (`0 0 9 * * MON`)
* **피드 소스**: 요즘IT, 카카오테크, 네이버D2, 우아한형제들, 토스
* **수집 제한**: 소스별 최대 저장 건수 `collectLimit`(기본 30), 기준 개월 이내 발행 `collectWithinMonths`(기본 6개월)
* **키워드 필터**: `TechArticleFilterKeywords` 내 개발 키워드 리스트(Spring, Java, React 등) 매칭 여부 검증
* **참고 및 갱신 컬럼 매핑**:
  - **`TECH_ARTICLE`** 테이블 (Upsert: `source` + `rssGuid` 조합으로 중복 여부 확인):
    - `title`: 글 제목
    - `summary`: 요약 내용
    - `thumbnailUrl`: 썸네일 이미지 URL (선택)
    - `author`: 저자
    - `articleUrl`: 원본 아티클 링크 URL
    - `publishedAt`: 발행 일시

---

## 3. 스케줄러 환경 제어 가이드
배치 활성화 및 스케줄 커스텀은 운영 환경 배포 시 환경 변수(`.env` 또는 AWS Elastic Beanstalk 환경 속성)를 주입하여 동적으로 조정할 수 있습니다.

```bash
# 스케줄러 활성화 (기본값 false, 운영에서만 true로 활성화 권장)
SCHEDULER_ENABLED=true

# 과정당 수강후기 크롤링 최대 페이지 상한 (기본값 10 = 최대 100건)
REVIEW_MAX_PAGES=10

# 필요 시 특정 배치의 스케줄 주기를 변경할 수 있습니다 (Spring Cron 문법 적용)
# HRD_COLLECT_CRON=0 0 2 * * MON
# HRD_REFINE_CRON=0 0 3 * * MON
# HRD_WEB_CRAWL_CRON=0 0 2 * * WED
# REVIEW_CRAWL_CRON=0 0 4 * * WED
# TECH_ARTICLE_COLLECT_CRON=0 0 9 * * MON
```
