package com.bootsignal.batch.job;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.crawled_review.entity.CrawledReview;
import com.bootsignal.domain.crawled_review.entity.CrawledReviewSource;
import com.bootsignal.domain.crawled_review.repository.CrawledReviewRepository;
import com.bootsignal.domain.work24.dto.ReviewCrawlResult;
import com.bootsignal.domain.work24.service.ReviewPageCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.Map;

/**
 * 고용24 수강후기 페이지를 크롤링하여 CrawledReview 에 저장하는 Job.
 *
 * 실행 전제: hrdDataRefineJob + hrdWebCrawlJob 이 완료되어
 *           CourseSession.titleLink 가 채워져 있어야 함.
 * 트리거:   POST /api/admin/batch/review-crawl
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReviewCrawlJobConfig {

    private final CourseRepository courseRepo;
    private final CourseSessionRepository courseSessionRepo;
    private final CrawledReviewRepository crawledReviewRepo;
    private final ReviewPageCrawlerService reviewCrawlerService;

    /** Course + 크롤링된 리뷰 목록을 Processor → Writer 간 전달하는 내부 DTO */
    record ReviewCrawlOutput(Course course, List<CrawledReview> reviews) {
    }

    // ═══════════════════════════════════════
    // Job 정의
    // ═══════════════════════════════════════

    @Bean
    public Job reviewCrawlJob(JobRepository jobRepository, Step crawlReviewStep) {
        return new JobBuilder("reviewCrawlJob", jobRepository)
                .start(crawlReviewStep)
                .build();
    }

    // ═══════════════════════════════════════
    // Step
    // ═══════════════════════════════════════

    @Bean
    public Step crawlReviewStep(JobRepository jobRepository,
                                PlatformTransactionManager txManager,
                                ItemProcessor<Course, ReviewCrawlOutput> reviewCrawlProcessor,
                                ItemWriter<ReviewCrawlOutput> reviewCrawlWriter) {
        return new StepBuilder("crawlReviewStep", jobRepository)
                .<Course, ReviewCrawlOutput>chunk(1, txManager)
                .reader(courseReaderForReviewCrawl())
                .processor(reviewCrawlProcessor)
                .writer(reviewCrawlWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    /**
     * reviewCrawledAt IS NULL 인 Course 를 pageSize=10 으로 읽는 Reader.
     */
    @Bean
    @StepScope
    public RepositoryItemReader<Course> courseReaderForReviewCrawl() {
        RepositoryItemReader<Course> reader = new RepositoryItemReader<>();
        reader.setRepository(courseRepo);
        reader.setMethodName("findByReviewCrawledAtIsNull");
        reader.setPageSize(10);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    /**
     * Course → 대표 세션의 titleLink로 수강후기 페이지 전체 크롤링 → ReviewCrawlOutput 반환.
     *
     * @param delayMillis 페이지 간 요청 딜레이(ms). Job Parameter 'delayMillis' 로 조정 가능.
     */
    @Bean
    @StepScope
    public ItemProcessor<Course, ReviewCrawlOutput> reviewCrawlProcessor(
            @Value("#{jobParameters['delayMillis'] ?: 500L}") Long delayMillis) {

        return course -> {
            String titleLink = courseSessionRepo
                    .findFirstByCourse_IdAndTitleLinkIsNotNull(course.getId())
                    .map(s -> s.getTitleLink())
                    .orElse(null);

            if (titleLink == null) {
                log.debug("titleLink 없음 - 건너뜀 (courseId={})", course.getId());
                return null;
            }

            try {
                List<ReviewCrawlResult> results =
                        reviewCrawlerService.fetchAndParseAllReviews(titleLink, delayMillis);

                Instant crawledAt = Instant.now();
                List<CrawledReview> newReviews = results.stream()
                        .filter(r -> !crawledReviewRepo.existsByCourseIdAndExternalReviewId(
                                course.getId(), r.externalReviewId()))
                        .map(r -> CrawledReview.builder()
                                .course(course)
                                .source(CrawledReviewSource.WORK24)
                                .externalReviewId(r.externalReviewId())
                                .reviewerNickname(r.reviewerNickname())
                                .rating(r.rating())
                                .content(r.content())
                                .reviewedAt(r.reviewedAt())
                                .crawledAt(crawledAt)
                                .build())
                        .toList();

                log.info("수강후기 크롤링 완료 (courseId={}, 전체={}건, 신규={}건)",
                        course.getId(), results.size(), newReviews.size());
                return new ReviewCrawlOutput(course, newReviews);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                log.warn("수강후기 크롤링 실패 (courseId={}, titleLink={}): {}",
                        course.getId(), titleLink, e.getMessage());
                return null;
            }
        };
    }

    /**
     * 신규 CrawledReview 저장 + Course.reviewCrawledAt 업데이트.
     * 리뷰가 없는 과정도 reviewCrawledAt 을 기록하여 재처리를 방지한다.
     */
    @Bean
    public ItemWriter<ReviewCrawlOutput> reviewCrawlWriter() {
        return items -> items.forEach(output -> {
            if (!output.reviews().isEmpty()) {
                crawledReviewRepo.saveAll(output.reviews());
            }
            output.course().markReviewCrawled(Instant.now());
            courseRepo.save(output.course());
        });
    }
}
