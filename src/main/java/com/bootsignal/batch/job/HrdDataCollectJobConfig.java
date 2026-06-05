package com.bootsignal.batch.job;

import com.bootsignal.batch.client.HrdApiClient;
import com.bootsignal.batch.dto.HrdCourseDetailApiResponse;
import com.bootsignal.batch.dto.HrdCourseListApiResponse.CourseListItem;
import com.bootsignal.batch.dto.HrdTrainingScheduleApiResponse;
import com.bootsignal.batch.reader.CourseListPagingReader;
import com.bootsignal.domain.hrd_raw.entity.HrdCourseDetailRaw;
import com.bootsignal.domain.hrd_raw.entity.HrdCourseListRaw;
import com.bootsignal.domain.hrd_raw.entity.HrdTrainingScheduleRaw;
import com.bootsignal.domain.hrd_raw.repository.HrdCourseDetailRawRepository;
import com.bootsignal.domain.hrd_raw.repository.HrdCourseListRawRepository;
import com.bootsignal.domain.hrd_raw.repository.HrdTrainingScheduleRawRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 고용24 데이터 수집 Job 설정.
 * 3개의 순차 Step으로 구성: 목록 수집 → 상세 수집 → 일정 수집
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HrdDataCollectJobConfig {

    private final HrdApiClient hrdApiClient;
    private final HrdCourseListRawRepository listRawRepo;
    private final HrdCourseDetailRawRepository detailRawRepo;
    private final HrdTrainingScheduleRawRepository scheduleRawRepo;

    // ═══════════════════════════════════════
    // Job 정의
    // ═══════════════════════════════════════

    @Bean
    public Job hrdDataCollectJob(JobRepository jobRepository,
                                 Step collectListStep,
                                 Step collectDetailStep,
                                 Step collectScheduleStep) {
        return new JobBuilder("hrdDataCollectJob", jobRepository)
                .start(collectListStep)
                .next(collectDetailStep)
                .next(collectScheduleStep)
                .build();
    }

    // ═══════════════════════════════════════
    // Step 1: 목록 API → HrdCourseListRaw
    // ═══════════════════════════════════════

    @Bean
    public Step collectListStep(JobRepository jobRepository,
                                PlatformTransactionManager txManager,
                                CourseListPagingReader courseListPagingReader) {
        return new StepBuilder("collectListStep", jobRepository)
                .<CourseListItem, HrdCourseListRaw>chunk(100, txManager)
                .reader(courseListPagingReader)
                .processor(courseListProcessor())
                .writer(courseListRawWriter())
                .build();
    }

    @Bean
    @StepScope
    public CourseListPagingReader courseListPagingReader(
            @Value("#{jobParameters['startDate']}") String startDate,
            @Value("#{jobParameters['endDate']}") String endDate) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        String start = startDate != null ? startDate : LocalDate.now().format(fmt);
        String end = endDate != null ? endDate : LocalDate.now().plusMonths(3).format(fmt);
        return new CourseListPagingReader(hrdApiClient, start, end);
    }

    // DTO → Entity 변환
    @Bean
    public ItemProcessor<CourseListItem, HrdCourseListRaw> courseListProcessor() {
        return item -> {
            // 이중 필터: TRAIN_TARGET_CD가 C0104(K-디지털 트레이닝)가 아니면 수집에서 제외
            if (!"C0104".equals(item.getTrainTargetCd())) {
                return null;
            }
            return HrdCourseListRaw.builder()
                    .trprId(item.getTrprId())
                    .trprDegr(item.getTrprDegr())
                    .trainstCstmrId(item.getTrainstCstmrId())
                    .title(item.getTitle())
                    .subTitle(item.getSubTitle())
                    .titleLink(item.getTitleLink())
                    .subTitleLink(item.getSubTitleLink())
                    .ncsCd(item.getNcsCd())
                    .courseMan(item.getCourseMan())
                    .yardMan(item.getYardMan())
                    .traStartDate(item.getTraStartDate())
                    .traEndDate(item.getTraEndDate())
                    .instCd(item.getInstCd())
                    .address(item.getAddress())
                    .trngAreaCd(item.getTrngAreaCd())
                    .realMan(item.getRealMan())
                    .stdgScor(item.getStdgScor())
                    .trainTargetCd(item.getTrainTargetCd())
                    .wkendSe(item.getWkendSe())
                    .regCourseMan(item.getRegCourseMan())
                    .fetchedAt(LocalDateTime.now())
                    .build();
        };
    }

    // Upsert: 기존 데이터가 있으면 갱신, 없으면 신규 저장
    @Bean
    public ItemWriter<HrdCourseListRaw> courseListRawWriter() {
        return items -> items.forEach(item ->
                listRawRepo.findByTrprIdAndTrprDegr(item.getTrprId(), item.getTrprDegr())
                        .ifPresentOrElse(
                                existing -> existing.updateFromApi(
                                        item.getTitle(), item.getSubTitle(), item.getTitleLink(),
                                        item.getSubTitleLink(), item.getNcsCd(), item.getCourseMan(),
                                        item.getYardMan(), item.getTraStartDate(), item.getTraEndDate(),
                                        item.getInstCd(), item.getAddress(), item.getTrngAreaCd(),
                                        item.getRealMan(), item.getStdgScor(), item.getTrainstCstmrId(),
                                        item.getTrainTargetCd(), item.getWkendSe(), item.getRegCourseMan()
                                ),
                                () -> listRawRepo.save(item)
                        )
        );
    }

    // ═══════════════════════════════════════
    // Step 2: HrdCourseListRaw → 상세 API → HrdCourseDetailRaw
    // ═══════════════════════════════════════

    @Bean
    public Step collectDetailStep(JobRepository jobRepository,
                                  PlatformTransactionManager txManager) {
        return new StepBuilder("collectDetailStep", jobRepository)
                .<HrdCourseListRaw, HrdCourseDetailRaw>chunk(10, txManager)
                .reader(listRawReaderForDetail())
                .processor(courseDetailProcessor())
                .writer(courseDetailRawWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE) // 개별 API 실패 시 해당 건만 건너뜀
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<HrdCourseListRaw> listRawReaderForDetail() {
        RepositoryItemReader<HrdCourseListRaw> reader = new RepositoryItemReader<>();
        reader.setRepository(listRawRepo);
        reader.setMethodName("findAll");
        reader.setPageSize(10);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    // 목록 Raw → 상세 API 호출 → 상세 Raw Entity 변환
    @Bean
    public ItemProcessor<HrdCourseListRaw, HrdCourseDetailRaw> courseDetailProcessor() {
        return listRaw -> {
            try {
                HrdCourseDetailApiResponse response = hrdApiClient.fetchCourseDetail(
                        listRaw.getTrprId(), listRaw.getTrprDegr(), listRaw.getTrainstCstmrId()
                );
                if (response == null || response.getInstBaseInfo() == null) return null;

                HrdCourseDetailApiResponse.InstBaseInfo base = response.getInstBaseInfo();
                HrdCourseDetailApiResponse.InstDetailInfo detail = response.getInstDetailInfo();

                return HrdCourseDetailRaw.builder()
                        .trprId(listRaw.getTrprId())
                        .trprDegr(listRaw.getTrprDegr())
                        .trprChap(base.getTrprChap())
                        .trprChapTel(base.getTrprChapTel())
                        .trprChapEmail(base.getTrprChapEmail())
                        .ncsYn(base.getNcsYn())
                        .ncsNm(base.getNcsNm())
                        .trDcnt(base.getTrDcnt())
                        .trtm(base.getTrtm())
                        .hpAddr(base.getHpAddr())
                        .tgcrGnrlTrneOwepAllt(detail != null ? detail.getTgcrGnrlTrneOwepAllt() : null)
                        .fetchedAt(LocalDateTime.now())
                        .build();
            } catch (Exception e) {
                log.warn("상세 API 실패 (trprId={}, degr={}): {}",
                        listRaw.getTrprId(), listRaw.getTrprDegr(), e.getMessage());
                return null; // null 반환 → 해당 건 건너뜀
            }
        };
    }

    @Bean
    public ItemWriter<HrdCourseDetailRaw> courseDetailRawWriter() {
        return items -> items.forEach(item ->
                detailRawRepo.findByTrprIdAndTrprDegr(item.getTrprId(), item.getTrprDegr())
                        .ifPresentOrElse(
                                existing -> existing.updateFromApi(
                                        item.getTrprChap(), item.getTrprChapTel(), item.getTrprChapEmail(),
                                        item.getNcsYn(), item.getNcsNm(), item.getTrDcnt(), item.getTrtm(),
                                        item.getHpAddr(), item.getTgcrGnrlTrneOwepAllt()
                                ),
                                () -> detailRawRepo.save(item)
                        )
        );
    }

    // ═══════════════════════════════════════
    // Step 3: HrdCourseListRaw → 일정 API → HrdTrainingScheduleRaw
    // ═══════════════════════════════════════

    @Bean
    public Step collectScheduleStep(JobRepository jobRepository,
                                    PlatformTransactionManager txManager) {
        return new StepBuilder("collectScheduleStep", jobRepository)
                .<HrdCourseListRaw, HrdTrainingScheduleRaw>chunk(10, txManager)
                .reader(listRawReaderForSchedule())
                .processor(scheduleProcessor())
                .writer(scheduleRawWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<HrdCourseListRaw> listRawReaderForSchedule() {
        RepositoryItemReader<HrdCourseListRaw> reader = new RepositoryItemReader<>();
        reader.setRepository(listRawRepo);
        reader.setMethodName("findAll");
        reader.setPageSize(10);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    // 목록 Raw → 일정 API 호출 → 일정 Raw Entity 변환
    @Bean
    public ItemProcessor<HrdCourseListRaw, HrdTrainingScheduleRaw> scheduleProcessor() {
        return listRaw -> {
            try {
                HrdTrainingScheduleApiResponse response = hrdApiClient.fetchTrainingSchedule(
                        listRaw.getTrprId(), listRaw.getTrprDegr(), listRaw.getTrainstCstmrId()
                );
                if (response == null || response.getFirstItem() == null) return null;

                HrdTrainingScheduleApiResponse.ScheduleItem schedule = response.getFirstItem();

                return HrdTrainingScheduleRaw.builder()
                        .trprId(listRaw.getTrprId())
                        .trprDegr(listRaw.getTrprDegr())
                        .eiEmplRate3(schedule.getEiEmplRate3())
                        .eiEmplRate6(schedule.getEiEmplRate6())
                        .totParMks(schedule.getTotParMks())
                        .finiCnt(schedule.getFiniCnt())
                        .fetchedAt(LocalDateTime.now())
                        .build();
            } catch (Exception e) {
                log.warn("일정 API 실패 (trprId={}, degr={}): {}",
                        listRaw.getTrprId(), listRaw.getTrprDegr(), e.getMessage());
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<HrdTrainingScheduleRaw> scheduleRawWriter() {
        return items -> items.forEach(item ->
                scheduleRawRepo.findByTrprIdAndTrprDegr(item.getTrprId(), item.getTrprDegr())
                        .ifPresentOrElse(
                                existing -> existing.updateFromApi(
                                        item.getEiEmplRate3(), item.getEiEmplRate6(),
                                        item.getTotParMks(), item.getFiniCnt()
                                ),
                                () -> scheduleRawRepo.save(item)
                        )
        );
    }
}
