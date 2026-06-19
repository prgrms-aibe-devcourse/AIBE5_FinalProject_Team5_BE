package com.bootsignal.domain.course.service;

import com.bootsignal.domain.code.service.FieldCategoryService;
import com.bootsignal.domain.course.dto.CourseDetailResponse;
import com.bootsignal.domain.course.dto.CourseListRequest;
import com.bootsignal.domain.course.dto.CourseListResponse;
import com.bootsignal.domain.course.dto.CourseSort;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.course_session.repository.CourseSessionSpecification;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.domain.crawled_review.repository.CrawledReviewRepository;
import com.bootsignal.global.dto.PageResponse;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 과정 목록, 과정 상세, 목록용 리뷰 평점 집계를 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    // 서비스 날짜 기준은 국내 과정 일정에 맞춰 한국 시간대를 사용합니다.
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final CourseRepository courseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final FieldCategoryService fieldCategoryService;
    private final ReviewRepository reviewRepository;
    private final CrawledReviewRepository crawledReviewRepository;

    /**
     * 과정 목록 조회 (검색 + 필터 + 페이징) - 과정 세션(기수) 기준 조회
     */
    public PageResponse<CourseListResponse> getCourses(CourseListRequest request) {
        CourseSort courseSort = CourseSort.from(request.sort());
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        Specification<CourseSession> spec = Specification.allOf(
                CourseSessionSpecification.withKeyword(request.keyword()),
                CourseSessionSpecification.withTrngAreaCd(request.trngAreaCd()),
                CourseSessionSpecification.withFieldCategory(request.fieldCategory(), fieldCategoryService),
                CourseSessionSpecification.withPriceRange(request.priceRange()),
                CourseSessionSpecification.withDurationFilter(request.durationFilter())
        );

        if (courseSort == CourseSort.POPULAR) {
            // 메인 인기 과정은 아직 시작하지 않은 과정만 노출하고, 북마크 수 기준으로 정렬합니다.
            spec = spec
                    .and(CourseSessionSpecification.startsOnOrAfter(today))
                    .and(CourseSessionSpecification.orderByBookmarkCountDesc());
        } else if (courseSort == CourseSort.DEADLINE) {
            // 모집 마감 임박순은 이미 시작한 과정보다 시작 예정 과정을 우선 노출합니다.
            spec = spec.and(CourseSessionSpecification.orderByDeadlineSoon(today));
        }

        Pageable pageable = PageRequest.of(request.page(), request.size(), toSort(courseSort));

        // N+1 방지를 위해 course와 institution을 fetch join
        Specification<CourseSession> withFetch = spec.and((root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                var courseFetch = root.fetch("course", jakarta.persistence.criteria.JoinType.LEFT);
                courseFetch.fetch("institution", jakarta.persistence.criteria.JoinType.LEFT);
            }
            return cb.conjunction();
        });

        Page<CourseSession> sessionPage = courseSessionRepository.findAll(withFetch, pageable);

        // 현재 페이지의 과정 ID 목록 추출 (중복 제거)
        List<Long> courseIds = sessionPage.getContent().stream()
                .map(session -> session.getCourse() != null ? session.getCourse().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // 각 과정별 통합 리뷰 평점 조회 및 계산
        Map<Long, BigDecimal> reviewRatings = calculateCombinedReviewRatings(courseIds);

        Page<CourseListResponse> responsePage = sessionPage.map(session -> {
            Long courseId = session.getCourse() != null ? session.getCourse().getId() : null;
            BigDecimal reviewRating = courseId != null
                    ? reviewRatings.getOrDefault(courseId, BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP))
                    : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
            return CourseListResponse.from(session, reviewRating);
        });

        return PageResponse.from(responsePage);
    }

    /**
     * 과정 상세 조회 (institution 및 대표 세션 포함)
     */
    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findWithInstitutionById(courseId)
                .orElseThrow(() -> new BootSignalException(ErrorCode.COURSE_NOT_FOUND));

        List<CourseSession> sessions = courseSessionRepository.findByCourse_IdOrderByTraStartDateAsc(courseId);
        java.time.LocalDate today = java.time.LocalDate.now();
        CourseSession repSession = CourseSession.findRepresentativeSession(sessions, today);

        return CourseDetailResponse.from(course, repSession);
    }

    private Map<Long, BigDecimal> calculateCombinedReviewRatings(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }

        // 1. 사용자 리뷰 통계 조회
        List<Object[]> userReviewSums = reviewRepository.findReviewSumsByCourseIds(courseIds);
        Map<Long, ReviewRatingStat> userStats = new HashMap<>();
        for (Object[] row : userReviewSums) {
            Long courseId = (Long) row[0];
            long count = toLong(row[1]);
            long sum = toLong(row[2]);
            userStats.put(courseId, new ReviewRatingStat(count, sum));
        }

        // 2. 크롤링 리뷰 통계 조회
        List<Object[]> crawledReviewSums = crawledReviewRepository.findCrawledReviewSumsByCourseIds(courseIds);
        Map<Long, ReviewRatingStat> crawledStats = new HashMap<>();
        for (Object[] row : crawledReviewSums) {
            Long courseId = (Long) row[0];
            long count = toLong(row[1]);
            long sum = toLong(row[2]);
            crawledStats.put(courseId, new ReviewRatingStat(count, sum));
        }

        // 3. 사용자 및 크롤링 리뷰 데이터 합산 및 평균 계산
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Long courseId : courseIds) {
            ReviewRatingStat uStat = userStats.getOrDefault(courseId, new ReviewRatingStat(0L, 0L));
            ReviewRatingStat cStat = crawledStats.getOrDefault(courseId, new ReviewRatingStat(0L, 0L));

            long totalCount = uStat.count() + cStat.count();
            long totalSum = uStat.sum() + cStat.sum();

            if (totalCount == 0) {
                result.put(courseId, BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            } else {
                BigDecimal avg = BigDecimal.valueOf(totalSum)
                        .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
                result.put(courseId, avg);
            }
        }

        return result;
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private Sort toSort(CourseSort courseSort) {
        return switch (courseSort) {
            // 북마크 수 집계 정렬은 Criteria orderBy로 처리하므로 Pageable Sort는 비워둡니다.
            case POPULAR -> Sort.unsorted();
            case SATISFACTION -> Sort.by(
                    desc("course.stdgScor"),
                    desc("id")
            );
            case EMPLOYMENT_RATE -> Sort.by(
                    desc("employmentRate"),
                    desc("id")
            );
            case DEADLINE -> Sort.unsorted();
            case LATEST -> Sort.by(desc("id"));
        };
    }

    private Sort.Order desc(String property) {
        return Sort.Order.desc(property);
    }

    private record ReviewRatingStat(long count, long sum) {}
}
