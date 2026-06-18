package com.bootsignal.batch.controller;

import com.bootsignal.batch.dto.BatchJobResponse;
import com.bootsignal.global.config.properties.HrdApiProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// 배치 Job 수동 트리거 Admin API
// HTTP 요청으로 배치 Job을 직접 실행 가능
// 정상 스케줄 외에 데이터 재수집이나 긴급 정제가 필요할 때 사용
//
//   POST /api/admin/batch/collect — 고용24 데이터 수집 Job 실행
//   POST /api/admin/batch/refine  — 수집 데이터 정제 Job 실행
//
//
@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
//@PreAuthorize("hasRole('ADMIN')")
public class BatchJobController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobLauncher jobLauncher;
    private final Job hrdDataCollectJob;
    private final Job hrdDataRefineJob;
    private final Job hrdWebCrawlJob;
    private final Job reviewCrawlJob;
    private final HrdApiProperties properties;

    public BatchJobController(
            JobLauncher jobLauncher,
            @Qualifier("hrdDataCollectJob") Job hrdDataCollectJob,
            @Qualifier("hrdDataRefineJob") Job hrdDataRefineJob,
            @Qualifier("hrdWebCrawlJob") Job hrdWebCrawlJob,
            @Qualifier("reviewCrawlJob") Job reviewCrawlJob,
            HrdApiProperties properties) {
        this.jobLauncher = jobLauncher;
        this.hrdDataCollectJob = hrdDataCollectJob;
        this.hrdDataRefineJob = hrdDataRefineJob;
        this.hrdWebCrawlJob = hrdWebCrawlJob;
        this.reviewCrawlJob = reviewCrawlJob;
        this.properties = properties;
    }

    /**
     * 수집 Job 실행 — 고용24 OpenAPI 원본 데이터를 Raw 테이블에 적재한다.
     *
     * @param startDate 훈련 시작일 from (yyyyMMdd). 미입력 시 오늘
     * @param endDate   훈련 시작일 to   (yyyyMMdd). 미입력 시 3개월 뒤
     */
    @PostMapping("/collect")
    public BatchJobResponse runCollectJob(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        String start = (startDate != null) ? startDate : LocalDate.now().format(DATE_FMT);
        String end   = (endDate   != null) ? endDate   : LocalDate.now().plusMonths(3).format(DATE_FMT);

        // API 인증키 주입 디버깅 로깅
        String key = properties.authKey();
        String maskedKey = (key != null && key.length() > 4) ? key.substring(0, 4) + "****" : "null/empty";
        log.info("[DEBUG] API Properties - authKey: {}, baseUrl: {}, returnType: {}", maskedKey, properties.baseUrl(), properties.returnType());

        JobParameters params = new JobParametersBuilder()
                .addString("startDate", start)
                .addString("endDate", end)
                .addLong("runAt", System.currentTimeMillis()) // 동일 파라미터로 재실행 허용
                .toJobParameters();

        return runJob(hrdDataCollectJob, "hrdDataCollectJob", params, start, end);
    }

    /**
     * 정제 Job 실행 — Raw 데이터를 Institution / Course / CourseSession 엔티티로 변환한다.
     * 수집 Job이 완료된 뒤 실행
     */
    @PostMapping("/refine")
    public BatchJobResponse runRefineJob() {
        JobParameters params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        return runJob(hrdDataRefineJob, "hrdDataRefineJob", params);
    }

    /**
     * 웹 크롤링 Job 실행 — Course.titleLink 기반으로 고용24 상세 페이지를 크롤링하여
     * Course / Institution / CourseSession 의 보조 정보 업데이트
     * 정제 Job이 완료된 뒤 실행
     *
     * @param delayMillis 과정 간 요청 딜레이(ms). 기본 1500ms. 너무 낮으면 IP 차단 위험.
     */
    @PostMapping("/web-crawl")
    public BatchJobResponse runWebCrawlJob(
            @RequestParam(required = false) Long delayMillis) {
        JobParameters params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .addLong("delayMillis", delayMillis != null ? delayMillis : 1500L)
                .toJobParameters();

        return runJob(hrdWebCrawlJob, "hrdWebCrawlJob", params);
    }

    /**
     * 수강후기 크롤링 Job 실행 — 고용24 수강후기 페이지에서 리뷰를 크롤링하여 crawled_review 테이블에 저장.
     * 웹 크롤링 Job이 완료된 뒤 실행 권장.
     *
     * @param delayMillis 페이지 간 요청 딜레이(ms). 기본 500ms.
     */
    @PostMapping("/review-crawl")
    public BatchJobResponse runReviewCrawlJob(
            @RequestParam(required = false) Long delayMillis) {
        JobParameters params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .addLong("delayMillis", delayMillis != null ? delayMillis : 500L)
                .toJobParameters();

        return runJob(reviewCrawlJob, "reviewCrawlJob", params);
    }

    /**
     * Job을 실행하고 결과를 반환하는 공통 헬퍼 (날짜 범위 포함).
     * 성공: BatchJobResponse - ApiResponseAdvice
     * 실패: BootSignalException -> GlobalExceptionHandler
     */
    private BatchJobResponse runJob(
            Job job, String jobName, JobParameters params,
            String startDate, String endDate) {
        try {
            JobExecution execution = jobLauncher.run(job, params);
            log.info("Job 실행 완료: name={}, status={}", jobName, execution.getStatus());
            return BatchJobResponse.success(jobName, execution.getStatus().toString(),
                    execution.getJobId(), startDate, endDate);
        } catch (Exception e) {
            log.error("Job 실행 실패: name={}, error={}", jobName, e.getMessage(), e);
            throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR,
                    jobName + " 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Job을 실행하고 결과를 반환하는 공통 헬퍼 (날짜 범위 없음).
     * 성공하면 BatchJobResponse 반환 — ApiResponseAdvice가 ApiResponse.success()로 자동 래핑.

     */
    private BatchJobResponse runJob(
            Job job, String jobName, JobParameters params) {
        try {
            JobExecution execution = jobLauncher.run(job, params);
            log.info("Job 실행 완료: name={}, status={}", jobName, execution.getStatus());
            return BatchJobResponse.success(jobName, execution.getStatus().toString(),
                    execution.getJobId());
        } catch (Exception e) {
            log.error("Job 실행 실패: name={}, error={}", jobName, e.getMessage(), e);
            throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR,
                    jobName + " 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
