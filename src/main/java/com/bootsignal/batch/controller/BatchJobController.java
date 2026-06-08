package com.bootsignal.batch.controller;

import com.bootsignal.batch.dto.BatchJobResponse;
import com.bootsignal.global.config.properties.HrdApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
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
//  TODO: 인증/인가 구현 후 @PreAuthorize("hasRole('ADMIN')") 추가
//
@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
public class BatchJobController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobLauncher jobLauncher;
    private final Job hrdDataCollectJob;
    private final Job hrdDataRefineJob;
    private final HrdApiProperties properties;

    public BatchJobController(
            JobLauncher jobLauncher,
            @Qualifier("hrdDataCollectJob") Job hrdDataCollectJob,
            @Qualifier("hrdDataRefineJob") Job hrdDataRefineJob,
            HrdApiProperties properties) {
        this.jobLauncher = jobLauncher;
        this.hrdDataCollectJob = hrdDataCollectJob;
        this.hrdDataRefineJob = hrdDataRefineJob;
        this.properties = properties;
    }

    /**
     * 수집 Job 실행 — 고용24 OpenAPI 원본 데이터를 Raw 테이블에 적재한다.
     *
     * @param startDate 훈련 시작일 from (yyyyMMdd). 미입력 시 오늘
     * @param endDate   훈련 시작일 to   (yyyyMMdd). 미입력 시 3개월 뒤
     */
    @PostMapping("/collect")
    public ResponseEntity<BatchJobResponse> runCollectJob(
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
    public ResponseEntity<BatchJobResponse> runRefineJob() {
        JobParameters params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        return runJob(hrdDataRefineJob, "hrdDataRefineJob", params);
    }

    /**
     * Job을 실행하고 결과를 HTTP 응답으로 변환하는 공통 헬퍼 (날짜 범위 포함).
     * 성공하면 200 OK, 예외가 발생하면 500 Internal Server Error를 반환한다.
     */
    private ResponseEntity<BatchJobResponse> runJob(
            Job job, String jobName, JobParameters params,
            String startDate, String endDate) {
        try {
            JobExecution execution = jobLauncher.run(job, params);
            log.info("Job 실행 완료: name={}, status={}", jobName, execution.getStatus());
            return ResponseEntity.ok(
                    BatchJobResponse.success(jobName, execution.getStatus().toString(),
                            execution.getJobId(), startDate, endDate));
        } catch (Exception e) {
            log.error("Job 실행 실패: name={}, error={}", jobName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(BatchJobResponse.error(jobName, e.getMessage()));
        }
    }

    /**
     * Job을 실행하고 결과를 HTTP 응답으로 변환하는 공통 헬퍼 (날짜 범위 없음).
     * 성공하면 200 OK, 예외가 발생하면 500 Internal Server Error를 반환한다.
     */
    private ResponseEntity<BatchJobResponse> runJob(
            Job job, String jobName, JobParameters params) {
        try {
            JobExecution execution = jobLauncher.run(job, params);
            log.info("Job 실행 완료: name={}, status={}", jobName, execution.getStatus());
            return ResponseEntity.ok(
                    BatchJobResponse.success(jobName, execution.getStatus().toString(),
                            execution.getJobId()));
        } catch (Exception e) {
            log.error("Job 실행 실패: name={}, error={}", jobName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(BatchJobResponse.error(jobName, e.getMessage()));
        }
    }
}
