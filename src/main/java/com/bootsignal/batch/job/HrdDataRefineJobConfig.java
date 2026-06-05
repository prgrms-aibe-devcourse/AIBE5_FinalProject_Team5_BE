package com.bootsignal.batch.job;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.hrd_raw.entity.HrdCourseDetailRaw;
import com.bootsignal.domain.hrd_raw.entity.HrdCourseListRaw;
import com.bootsignal.domain.hrd_raw.entity.HrdTrainingScheduleRaw;
import com.bootsignal.domain.hrd_raw.repository.HrdCourseDetailRawRepository;
import com.bootsignal.domain.hrd_raw.repository.HrdCourseListRawRepository;
import com.bootsignal.domain.hrd_raw.repository.HrdTrainingScheduleRawRepository;
import com.bootsignal.domain.institution.entity.Institution;
import com.bootsignal.domain.institution.repository.InstitutionRepository;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Raw 테이블 데이터를 서비스 엔티티(Institution, Course, CourseSession)로 정제하는 Job.
 * hrdDataCollectJob(수집) 완료 후 실행
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HrdDataRefineJobConfig {

    private final HrdCourseListRawRepository listRawRepo;
    private final HrdCourseDetailRawRepository detailRawRepo;
    private final HrdTrainingScheduleRawRepository scheduleRawRepo;
    private final InstitutionRepository institutionRepo;
    private final CourseRepository courseRepo;
    private final CourseSessionRepository courseSessionRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ═══════════════════════════════════════
    // Job 정의
    // ═══════════════════════════════════════

    @Bean
    public Job hrdDataRefineJob(JobRepository jobRepository,
                                Step refineInstitutionStep,
                                Step refineCourseStep,
                                Step refineCourseSessionStep) {
        return new JobBuilder("hrdDataRefineJob", jobRepository)
                .start(refineInstitutionStep)
                .next(refineCourseStep)
                .next(refineCourseSessionStep)
                .build();
    }

    // ═══════════════════════════════════════
    // Step 1: HrdCourseListRaw + Detail → Institution
    // ═══════════════════════════════════════

    @Bean
    public Step refineInstitutionStep(JobRepository jobRepository,
                                      PlatformTransactionManager txManager) {
        return new StepBuilder("refineInstitutionStep", jobRepository)
                .<HrdCourseListRaw, Institution>chunk(50, txManager)
                .reader(listRawReaderForInstitution())
                .processor(institutionProcessor())
                .writer(institutionWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<HrdCourseListRaw> listRawReaderForInstitution() {
        RepositoryItemReader<HrdCourseListRaw> reader = new RepositoryItemReader<>();
        reader.setRepository(listRawRepo);
        reader.setMethodName("findAll");
        reader.setPageSize(50);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    @Bean
    public ItemProcessor<HrdCourseListRaw, Institution> institutionProcessor() {
        return listRaw -> {
            // instCd가 없으면 Institution을 만들 수 없으므로 건너뜀
            if (listRaw.getInstCd() == null || listRaw.getInstCd().isBlank()) return null;

            // 상세 Raw에서 담당자 정보 조회 (없을 수 있으므로 Optional 처리)
            HrdCourseDetailRaw detail = detailRawRepo
                    .findByTrprIdAndTrprDegr(listRaw.getTrprId(), listRaw.getTrprDegr())
                    .orElse(null);

            // 이미 존재하는 Institution이면 최신 정보로 갱신(Upsert)
            Institution existing = institutionRepo.findByInstCd(listRaw.getInstCd()).orElse(null);
            if (existing != null) {
                existing.updateFromRaw(
                        listRaw.getSubTitle(),
                        listRaw.getAddress(),
                        detail != null ? detail.getHpAddr() : null,
                        detail != null ? detail.getTrprChap() : null,
                        detail != null ? detail.getTrprChapTel() : null,
                        detail != null ? detail.getTrprChapEmail() : null
                );
                return existing; // Dirty Checking + save() → UPDATE 쿼리 발생
            }

            return Institution.builder()
                    .instCd(listRaw.getInstCd())
                    .institutionName(listRaw.getSubTitle())
                    .address(listRaw.getAddress())
                    .homepageUrl(detail != null ? detail.getHpAddr() : null)
                    .managerName(detail != null ? detail.getTrprChap() : null)
                    .managerTel(detail != null ? detail.getTrprChapTel() : null)
                    .managerEmail(detail != null ? detail.getTrprChapEmail() : null)
                    .build();
        };
    }

    @Bean
    public ItemWriter<Institution> institutionWriter() {
        return items -> items.forEach(institutionRepo::save);
    }

    // ═══════════════════════════════════════
    // Step 2: HrdCourseListRaw + Detail → Course
    // ═══════════════════════════════════════

    @Bean
    public Step refineCourseStep(JobRepository jobRepository,
                                 PlatformTransactionManager txManager) {
        return new StepBuilder("refineCourseStep", jobRepository)
                .<HrdCourseListRaw, Course>chunk(50, txManager)
                .reader(listRawReaderForCourse())
                .processor(courseProcessor())
                .writer(courseWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<HrdCourseListRaw> listRawReaderForCourse() {
        RepositoryItemReader<HrdCourseListRaw> reader = new RepositoryItemReader<>();
        reader.setRepository(listRawRepo);
        reader.setMethodName("findAll");
        reader.setPageSize(50);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    @Bean
    public ItemProcessor<HrdCourseListRaw, Course> courseProcessor() {
        return listRaw -> {
            HrdCourseDetailRaw detail = detailRawRepo
                    .findByTrprIdAndTrprDegr(listRaw.getTrprId(), listRaw.getTrprDegr())
                    .orElse(null);

            Institution institution = institutionRepo.findByInstCd(listRaw.getInstCd())
                    .orElse(null);

            Course existing = courseRepo.findByTrprId(listRaw.getTrprId()).orElse(null);
            if (existing != null) {
                // 기존 Course 갱신
                existing.updateFromRaw(
                        listRaw.getTitle(), listRaw.getSubTitle(),
                        listRaw.getTitleLink(), listRaw.getSubTitleLink(),
                        listRaw.getNcsCd(),
                        detail != null ? detail.getNcsNm() : null,
                        detail != null ? detail.getNcsYn() : null,
                        parseBigDecimal(listRaw.getCourseMan()),
                        parseBigDecimal(listRaw.getRealMan()),
                        detail != null ? parseBigDecimal(detail.getTgcrGnrlTrneOwepAllt()) : null,
                        parseBigDecimal(listRaw.getStdgScor()),
                        detail != null ? parseInteger(detail.getTrDcnt()) : null,
                        detail != null ? parseInteger(detail.getTrtm()) : null,
                        listRaw.getTrngAreaCd(), institution
                );
                return null; // Processor가 null 반환 → Writer에 전달 안 됨 (이미 Dirty Checking으로 처리)
            }

            // 신규 Course 생성
            return Course.builder()
                    .trprId(listRaw.getTrprId())
                    .title(listRaw.getTitle())
                    .subTitle(listRaw.getSubTitle())
                    .titleLink(listRaw.getTitleLink())
                    .subTitleLink(listRaw.getSubTitleLink())
                    .ncsCd(listRaw.getNcsCd())
                    .ncsName(detail != null ? detail.getNcsNm() : null)
                    .ncsYn(detail != null ? detail.getNcsYn() : null)
                    .courseMan(parseBigDecimal(listRaw.getCourseMan()))
                    .realMan(parseBigDecimal(listRaw.getRealMan()))
                    .selfPaymentAmount(detail != null ? parseBigDecimal(detail.getTgcrGnrlTrneOwepAllt()) : null)
                    .stdgScor(parseBigDecimal(listRaw.getStdgScor()))
                    .totalTrainingDays(detail != null ? parseInteger(detail.getTrDcnt()) : null)
                    .totalTrainingHours(detail != null ? parseInteger(detail.getTrtm()) : null)
                    .trngAreaCd(listRaw.getTrngAreaCd())
                    .institution(institution)
                    .build();
        };
    }

    @Bean
    public ItemWriter<Course> courseWriter() {
        return items -> items.forEach(courseRepo::save);
    }

    // ═══════════════════════════════════════
    // Step 3: HrdCourseListRaw + Schedule → CourseSession
    // ═══════════════════════════════════════

    @Bean
    public Step refineCourseSessionStep(JobRepository jobRepository,
                                        PlatformTransactionManager txManager) {
        return new StepBuilder("refineCourseSessionStep", jobRepository)
                .<HrdCourseListRaw, CourseSession>chunk(50, txManager)
                .reader(listRawReaderForSession())
                .processor(courseSessionProcessor())
                .writer(courseSessionWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<HrdCourseListRaw> listRawReaderForSession() {
        RepositoryItemReader<HrdCourseListRaw> reader = new RepositoryItemReader<>();
        reader.setRepository(listRawRepo);
        reader.setMethodName("findAll");
        reader.setPageSize(50);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    @Bean
    public ItemProcessor<HrdCourseListRaw, CourseSession> courseSessionProcessor() {
        return listRaw -> {
            // Course가 없으면 FK를 연결할 수 없으므로 건너뜀
            Course course = courseRepo.findByTrprId(listRaw.getTrprId()).orElse(null);
            if (course == null) return null;

            HrdTrainingScheduleRaw schedule = scheduleRawRepo
                    .findByTrprIdAndTrprDegr(listRaw.getTrprId(), listRaw.getTrprDegr())
                    .orElse(null);

            CourseSession existing = courseSessionRepo
                    .findByCourse_TrprIdAndTrprDegr(listRaw.getTrprId(), listRaw.getTrprDegr())
                    .orElse(null);

            if (existing != null) {
                existing.updateFromRaw(
                        parseDate(listRaw.getTraStartDate()),
                        parseDate(listRaw.getTraEndDate()),
                        parseInteger(listRaw.getYardMan()),
                        parseInteger(listRaw.getRegCourseMan()),
                        schedule != null ? parseInteger(schedule.getTotParMks()) : null,
                        schedule != null ? parseInteger(schedule.getFiniCnt()) : null,
                        schedule != null ? schedule.getEiEmplRate3() : null,
                        schedule != null ? schedule.getEiEmplRate6() : null,
                        listRaw.getWkendSe()
                );
                return existing; // null 대신 반환 → Writer의 save()로 명시적 UPDATE 보장
            }

            return CourseSession.builder()
                    .trprDegr(listRaw.getTrprDegr())
                    .traStartDate(parseDate(listRaw.getTraStartDate()))
                    .traEndDate(parseDate(listRaw.getTraEndDate()))
                    .yardMan(parseInteger(listRaw.getYardMan()))
                    .regCourseMan(parseInteger(listRaw.getRegCourseMan()))
                    .totParMks(schedule != null ? parseInteger(schedule.getTotParMks()) : null)
                    .finiCnt(schedule != null ? parseInteger(schedule.getFiniCnt()) : null)
                    .eiEmplRate3(schedule != null ? schedule.getEiEmplRate3() : null)
                    .eiEmplRate6(schedule != null ? schedule.getEiEmplRate6() : null)
                    .wkendSe(listRaw.getWkendSe())
                    .course(course)
                    .build();
        };
    }

    @Bean
    public ItemWriter<CourseSession> courseSessionWriter() {
        return items -> items.forEach(courseSessionRepo::save);
    }

    // ═══════════════════════════════════════
    // 타입 변환 유틸 (String → BigDecimal / Integer / LocalDate)
    // Raw 엔티티는 모든 값을 String으로 저장하므로, 변환 실패 시 null 반환
    // ═══════════════════════════════════════

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            log.warn("BigDecimal 변환 실패: '{}'", value);
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim().replace(",", "").split("\\.")[0]);
        } catch (NumberFormatException e) {
            log.warn("Integer 변환 실패: '{}'", value);
            return null;
        }
    }

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyyMMdd"),   // 20260101
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 2026-01-01
            DateTimeFormatter.ofPattern("yyyy.MM.dd")  // 2026.01.01
    );

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
                // 다음 포맷으로 재시도
            }
        }
        log.warn("LocalDate 변환 실패 (지원되지 않는 형식): '{}'", trimmed);
        return null;
    }
}
