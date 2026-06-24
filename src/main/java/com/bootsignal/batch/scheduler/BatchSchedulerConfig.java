package com.bootsignal.batch.scheduler;

import com.bootsignal.domain.tech_article.entity.ArticleSource;
import com.bootsignal.domain.tech_article.service.TechArticleCollectService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 배치 Job 자동 스케줄러.
 *
 * SCHEDULER_ENABLED=true 환경변수로 명시적으로 활성화해야 동작한다.
 * Single Instance (EB 기본 구성) 환경에서 AtomicBoolean으로 중복 실행을 방지한다.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ Job 실행 순서 (의존관계) │
 * │ │
 * │ 매주 월 02:00 hrdDataCollectJob (HRD API → Raw 테이블) │
 * │ 매주 월 03:00 hrdDataRefineJob (Raw → Course/Session) │
 * │ 매주 수 02:00 hrdWebCrawlJob (HTML 크롤링 보강) │
 * │ 매주 수 04:00 reviewCrawlJob (수강후기 크롤링) │
 * │ 매주 월 09:00 TechArticle 수집 (RSS 피드) │
 * └─────────────────────────────────────────────────────────┘
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class BatchSchedulerConfig {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobLauncher asyncJobLauncher;
    private final Job hrdDataCollectJob;
    private final Job hrdDataRefineJob;
    private final Job hrdWebCrawlJob;
    private final Job reviewCrawlJob;
    private final TechArticleCollectService techArticleCollectService;

    // maxPages: 과정당 수강후기 최대 크롤링 페이지 수 (기본 10 = 최대 100건)
    @Value("${app.scheduler.review-max-pages:10}")
    private int reviewMaxPages;

    // Single Instance 환경에서 Job 중복 실행 방지 플래그
    private final AtomicBoolean collectRunning = new AtomicBoolean(false);
    private final AtomicBoolean refineRunning = new AtomicBoolean(false);
    private final AtomicBoolean webCrawlRunning = new AtomicBoolean(false);
    private final AtomicBoolean reviewCrawlRunning = new AtomicBoolean(false);
    private final AtomicBoolean techArticleRunning = new AtomicBoolean(false);

    public BatchSchedulerConfig(
            JobLauncher asyncJobLauncher,
            @Qualifier("hrdDataCollectJob") Job hrdDataCollectJob,
            @Qualifier("hrdDataRefineJob") Job hrdDataRefineJob,
            @Qualifier("hrdWebCrawlJob") Job hrdWebCrawlJob,
            @Qualifier("reviewCrawlJob") Job reviewCrawlJob,
            TechArticleCollectService techArticleCollectService) {
        this.asyncJobLauncher = asyncJobLauncher;
        this.hrdDataCollectJob = hrdDataCollectJob;
        this.hrdDataRefineJob = hrdDataRefineJob;
        this.hrdWebCrawlJob = hrdWebCrawlJob;
        this.reviewCrawlJob = reviewCrawlJob;
        this.techArticleCollectService = techArticleCollectService;
    }

    /**
     * 스케줄러 전용 비동기 JobLauncher.
     * 스케줄러 스레드가 Job 완료까지 블로킹되지 않도록 SimpleAsyncTaskExecutor를 사용한다.
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("batch-scheduler-"));
        launcher.afterPropertiesSet();
        return launcher;
    }

    // ═══════════════════════════════════════
    // ① hrdDataCollectJob — 매일 02:00
    // ═══════════════════════════════════════

    @Scheduled(cron = "${app.scheduler.hrd-collect-cron:0 0 2 * * MON}")
    public void scheduleHrdCollect() {
        if (!collectRunning.compareAndSet(false, true)) {
            log.warn("[Scheduler] hrdDataCollectJob 이미 실행 중 — 스킵");
            return;
        }
        try {
            String today = LocalDate.now().format(DATE_FMT);
            String threeMonthsLater = LocalDate.now().plusMonths(3).format(DATE_FMT);

            JobParameters params = new JobParametersBuilder()
                    .addString("startDate", today)
                    .addString("endDate", threeMonthsLater)
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters();

            log.info("[Scheduler] hrdDataCollectJob 시작 (startDate={}, endDate={})", today, threeMonthsLater);
            asyncJobLauncher.run(hrdDataCollectJob, params);
        } catch (Exception e) {
            log.error("[Scheduler] hrdDataCollectJob 실행 실패: {}", e.getMessage(), e);
        } finally {
            collectRunning.set(false);
        }
    }

    // ═══════════════════════════════════════
    // ② hrdDataRefineJob — 매일 03:00
    // ═══════════════════════════════════════

    @Scheduled(cron = "${app.scheduler.hrd-refine-cron:0 0 3 * * MON}")
    public void scheduleHrdRefine() {
        if (!refineRunning.compareAndSet(false, true)) {
            log.warn("[Scheduler] hrdDataRefineJob 이미 실행 중 — 스킵");
            return;
        }
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters();

            log.info("[Scheduler] hrdDataRefineJob 시작");
            asyncJobLauncher.run(hrdDataRefineJob, params);
        } catch (Exception e) {
            log.error("[Scheduler] hrdDataRefineJob 실행 실패: {}", e.getMessage(), e);
        } finally {
            refineRunning.set(false);
        }
    }

    // ═══════════════════════════════════════
    // ③ hrdWebCrawlJob — 매주 일요일 04:00
    // ═══════════════════════════════════════

    @Scheduled(cron = "${app.scheduler.hrd-web-crawl-cron:0 0 2 * * WED}")
    public void scheduleHrdWebCrawl() {
        if (!webCrawlRunning.compareAndSet(false, true)) {
            log.warn("[Scheduler] hrdWebCrawlJob 이미 실행 중 — 스킵");
            return;
        }
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .addLong("delayMillis", 1500L)
                    .toJobParameters();

            log.info("[Scheduler] hrdWebCrawlJob 시작");
            asyncJobLauncher.run(hrdWebCrawlJob, params);
        } catch (Exception e) {
            log.error("[Scheduler] hrdWebCrawlJob 실행 실패: {}", e.getMessage(), e);
        } finally {
            webCrawlRunning.set(false);
        }
    }

    // ═══════════════════════════════════════
    // ④ reviewCrawlJob — 매주 일요일 06:00
    // ═══════════════════════════════════════

    @Scheduled(cron = "${app.scheduler.review-crawl-cron:0 0 4 * * WED}")
    public void scheduleReviewCrawl() {
        if (!reviewCrawlRunning.compareAndSet(false, true)) {
            log.warn("[Scheduler] reviewCrawlJob 이미 실행 중 — 스킵");
            return;
        }
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .addLong("delayMillis", 500L)
                    .addLong("maxPages", (long) reviewMaxPages)
                    .toJobParameters();

            log.info("[Scheduler] reviewCrawlJob 시작 (maxPages={})", reviewMaxPages);
            asyncJobLauncher.run(reviewCrawlJob, params);
        } catch (Exception e) {
            log.error("[Scheduler] reviewCrawlJob 실행 실패: {}", e.getMessage(), e);
        } finally {
            reviewCrawlRunning.set(false);
        }
    }

    // ═══════════════════════════════════════
    // ⑤ TechArticle RSS 수집 — 매일 09:00
    // ═══════════════════════════════════════

    @Scheduled(cron = "${app.scheduler.tech-article-collect-cron:0 0 9 * * MON}")
    public void scheduleTechArticleCollect() {
        if (!techArticleRunning.compareAndSet(false, true)) {
            log.warn("[Scheduler] TechArticle 수집 이미 실행 중 — 스킵");
            return;
        }
        try {
            log.info("[Scheduler] TechArticle RSS 수집 시작 (전체 소스)");
            techArticleCollectService.collect(null); // null = 전체 소스 수집
            log.info("[Scheduler] TechArticle RSS 수집 완료");
        } catch (Exception e) {
            log.error("[Scheduler] TechArticle RSS 수집 실패: {}", e.getMessage(), e);
        } finally {
            techArticleRunning.set(false);
        }
    }
}
