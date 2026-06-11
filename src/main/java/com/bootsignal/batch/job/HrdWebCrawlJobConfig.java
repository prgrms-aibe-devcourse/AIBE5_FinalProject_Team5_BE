package com.bootsignal.batch.job;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.institution.entity.Institution;
import com.bootsignal.domain.institution.repository.InstitutionRepository;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverview;
import com.bootsignal.domain.work24.service.Work24CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;

/**
 * 고용24 훈련과정 상세 페이지를 HTML 크롤링하여
 * Course / Institution / CourseSession 의 보조 정보를 채우는 Job.
 *
 * 실행 전제: hrdDataRefineJob 이 완료되어 Course.titleLink 가 채워져 있어야 함.
 * 트리거:   POST /api/admin/batch/web-crawl
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HrdWebCrawlJobConfig {

    private final CourseRepository courseRepo;
    private final CourseSessionRepository courseSessionRepo;
    private final InstitutionRepository institutionRepo;
    private final Work24CrawlerService crawlerService;

    // ═══════════════════════════════════════
    // Job 정의
    // ═══════════════════════════════════════

    @Bean
    public Job hrdWebCrawlJob(JobRepository jobRepository, Step crawlCourseOverviewStep) {
        return new JobBuilder("hrdWebCrawlJob", jobRepository)
                .start(crawlCourseOverviewStep)
                .build();
    }

    // ═══════════════════════════════════════
    // Step: Course.titleLink → HTML 크롤링 → 엔티티 업데이트
    // ═══════════════════════════════════════

    @Bean
    public Step crawlCourseOverviewStep(JobRepository jobRepository,
                                        PlatformTransactionManager txManager,
                                        ItemProcessor<CourseSession, Work24TrainingCourseOverview> crawlProcessor,
                                        ItemWriter<Work24TrainingCourseOverview> crawlWriter) {
        return new StepBuilder("crawlCourseOverviewStep", jobRepository)
                .<CourseSession, Work24TrainingCourseOverview>chunk(1, txManager)  // 크롤링은 1건씩 처리
                .reader(courseSessionReaderForCrawl())
                .processor(crawlProcessor)
                .writer(crawlWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)  // 개별 크롤링 실패는 건너뜀
                .build();
    }

    /**
     * titleLink 가 있는 CourseSession 전체를 pageSize=10 으로 읽는 Reader.
     */
    @Bean
    @StepScope
    public RepositoryItemReader<CourseSession> courseSessionReaderForCrawl() {
        RepositoryItemReader<CourseSession> reader = new RepositoryItemReader<>();
        reader.setRepository(courseSessionRepo);
        reader.setMethodName("findAll");
        reader.setPageSize(10);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    /**
     * CourseSession.titleLink → HTML 크롤링 → Work24TrainingCourseOverview DTO 반환.
     *
     * @param delayMillis 과정 간 요청 딜레이(ms). Job Parameter 'delayMillis' 로 조정 가능. 기본 1500ms.
     */
    @Bean
    @StepScope
    public ItemProcessor<CourseSession, Work24TrainingCourseOverview> crawlProcessor(
            @Value("#{jobParameters['delayMillis'] ?: 1500L}") Long delayMillis) {

        return session -> {
            String titleLink = session.getTitleLink();
            if (titleLink == null || titleLink.isBlank()) {
                log.debug("titleLink 없음 - 건너뜀 (trprId={}, degr={})", session.getTrprId(), session.getTrprDegr());
                return null;
            }

            try {
                // 1) 훈련과정 상세 페이지 GET
                Document document = Jsoup.connect(titleLink)
                        .userAgent(crawlerService.getUserAgent())
                        .timeout(crawlerService.getTimeoutMillis())
                        .get();

                // 2) 기관정보 탭 POST (내부적으로 파라미터 조립)
                Document institutionDocument = crawlerService.fetchInstitutionDocument(document, titleLink);

                // 3) 파싱 (파일 저장 없이 DTO만 반환)
                Work24TrainingCourseOverview overview =
                        crawlerService.parse(document, institutionDocument, titleLink, Instant.now());

                log.info("크롤링 완료 (trprId={}, degr={}, url={})", session.getTrprId(), session.getTrprDegr(), titleLink);

                // 4) 요청 간격 준수
                Thread.sleep(delayMillis);

                return overview;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                log.warn("크롤링 실패 (trprId={}, degr={}, url={}): {}", session.getTrprId(), session.getTrprDegr(), titleLink, e.getMessage());
                return null;  // null 반환 → Writer 에 전달 안 됨 (해당 건 건너뜀)
            }
        };
    }

    /**
     * 크롤링 결과(overview)를 Course / Institution / CourseSession 에 반영하는 Writer.
     */
    @Bean
    public ItemWriter<Work24TrainingCourseOverview> crawlWriter() {
        return items -> items.forEach(overview -> {
            // CourseSession 조회 (sourceUrl 에서 역추적)
            courseSessionRepo.findByTitleLink(overview.sourceUrl()).ifPresentOrElse(session -> {

                // ── CourseSession 업데이트 ───────────────────────
                session.updateFromCrawl(
                        overview.selectedTraineeCount(),
                        overview.recruitmentCount(),
                        overview.confirmedTraineeCount(),
                        overview.employmentRate()
                );
                courseSessionRepo.save(session);

                // ── Course 업데이트 ──────────────────────────────
                Course course = session.getCourse();
                if (course != null) {
                    course.updateFromCrawl(
                            overview.trainingTargetRequirements(),
                            overview.trainingGoal(),
                            overview.crawledAt()
                    );
                    courseRepo.save(course);

                    // ── Institution 업데이트 ─────────────────────────
                    Institution institution = course.getInstitution();
                    if (institution != null) {
                        institution.updateFromCrawl(
                                overview.institutionProfileImageUrl(),
                                overview.institutionIntroduction()
                        );
                        institutionRepo.save(institution);
                    }
                }

            }, () -> log.warn("CourseSession 을 찾을 수 없음 (sourceUrl={})", overview.sourceUrl()));
        });
    }
}
