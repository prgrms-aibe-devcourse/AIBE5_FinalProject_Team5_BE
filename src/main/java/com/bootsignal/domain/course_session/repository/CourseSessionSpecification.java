package com.bootsignal.domain.course_session.repository;

import com.bootsignal.domain.code.service.FieldCategoryService;
import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.course.dto.DurationFilter;
import com.bootsignal.domain.course.dto.PriceRange;
import com.bootsignal.domain.course.dto.FieldCategory;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.crawled_review.entity.CrawledReview;
import com.bootsignal.domain.review.entity.Review;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 과정 목록 조회에 필요한 검색, 필터, 정렬 조건을 JPA Specification으로 조립하는 파일입니다.
 */
public class CourseSessionSpecification {

    private CourseSessionSpecification() {
    }

    /**
     * 과정명(course.title) 또는 기관명(course.institution.institutionName) 부분 일치 검색
     */
    public static Specification<CourseSession> withKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            var courseJoin = root.join("course", JoinType.LEFT);
            var institutionJoin = courseJoin.join("institution", JoinType.LEFT);
            return cb.or(
                    cb.like(courseJoin.get("title"), "%" + keyword + "%"),
                    cb.like(institutionJoin.get("institutionName"), "%" + keyword + "%")
            );
        };
    }

    /**
     * 지역 대분류 코드 전방 일치 필터
     * 예) trngAreaCd="11" → course.trngAreaCd LIKE '11%' (서울 전체)
     */
    public static Specification<CourseSession> withTrngAreaCd(String trngAreaCd) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(trngAreaCd)) {
                return null;
            }
            var courseJoin = root.join("course", JoinType.LEFT);
            return cb.like(courseJoin.get("trngAreaCd"), trngAreaCd + "%");
        };
    }

    /**
     * 분야 카테고리 필터 (NCS 코드 기반 IN / NOT IN 쿼리)
     * - 일반 카테고리: course.ncsCd IN (해당 카테고리 코드 목록)
     * - OTHERS: course.ncsCd NOT IN (7개 카테고리 전체 코드 목록) OR course.ncsCd IS NULL
     */
    public static Specification<CourseSession> withFieldCategory(FieldCategory fieldCategory,
                                                                 FieldCategoryService fieldCategoryService) {
        return (root, query, cb) -> {
            if (fieldCategory == null) {
                return null;
            }
            var courseJoin = root.join("course", JoinType.LEFT);
            if (fieldCategory == FieldCategory.OTHERS) {
                Set<String> allCodes = fieldCategoryService.getAllCategorizedCodes();
                if (allCodes.isEmpty()) {
                    return null;
                }
                return cb.or(
                        courseJoin.get("ncsCd").isNull(),
                        cb.not(courseJoin.get("ncsCd").in(allCodes))
                );
            }
            Set<String> codes = fieldCategoryService.getCodesFor(fieldCategory);
            if (codes == null || codes.isEmpty()) {
                return null;
            }
            return courseJoin.get("ncsCd").in(codes);
        };
    }

    /**
     * 가격 범위 필터 — CourseSession.selfPaymentAmount 기준
     * BELOW_30 : 0 ~ 300,000 이하
     * BELOW_45 : 0 ~ 450,000 이하
     * BELOW_60 : 0 ~ 600,000 이하
     */
    public static Specification<CourseSession> withPriceRange(PriceRange priceRange) {
        return (root, query, cb) -> {
            if (priceRange == null) {
                return null;
            }
            return switch (priceRange) {
                case BELOW_30 -> cb.lessThanOrEqualTo(root.get("selfPaymentAmount"), 300000);
                case BELOW_45 -> cb.lessThanOrEqualTo(root.get("selfPaymentAmount"), 450000);
                case BELOW_60 -> cb.lessThanOrEqualTo(root.get("selfPaymentAmount"), 600000);
            };
        };
    }

    /**
     * 기간 필터 — CourseSession.totalTrainingDays 기준
     * WITHIN_3_MONTHS : totalTrainingDays <= 90
     * WITHIN_6_MONTHS : 91 <= totalTrainingDays <= 180
     * OVER_6_MONTHS   : totalTrainingDays > 180
     */
    public static Specification<CourseSession> withDurationFilter(DurationFilter durationFilter) {
        return (root, query, cb) -> {
            if (durationFilter == null) {
                return null;
            }
            return switch (durationFilter) {
                case WITHIN_3_MONTHS -> cb.lessThanOrEqualTo(root.get("totalTrainingDays"), 90);
                case WITHIN_6_MONTHS -> cb.and(
                        cb.greaterThan(root.get("totalTrainingDays"), 90),
                        cb.lessThanOrEqualTo(root.get("totalTrainingDays"), 180)
                );
                case OVER_6_MONTHS -> cb.greaterThan(root.get("totalTrainingDays"), 180);
            };
        };
    }

    /**
     * 기준일 이후에 시작하는 과정 기수만 조회합니다.
     * 메인 인기 과정은 아직 시작하지 않은 과정만 노출해야 하므로 오늘 날짜를 기준으로 사용합니다.
     */
    public static Specification<CourseSession> startsOnOrAfter(LocalDate startDate) {
        return (root, query, cb) -> {
            if (startDate == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("traStartDate"), startDate);
        };
    }

    /**
     * 북마크 수가 많은 과정 기수부터 정렬합니다.
     * 별도 인기 점수 컬럼이 없으므로 Bookmark 테이블의 courseSession 집계 수를 인기 기준으로 사용합니다.
     */
    public static Specification<CourseSession> orderByBookmarkCountDesc() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                Subquery<Long> bookmarkCount = query.subquery(Long.class);
                Root<Bookmark> bookmark = bookmarkCount.from(Bookmark.class);
                bookmarkCount.select(cb.count(bookmark.get("id")));
                bookmarkCount.where(cb.equal(bookmark.get("courseSession").get("id"), root.get("id")));

                var courseJoin = root.join("course", JoinType.LEFT);
                query.orderBy(
                        cb.desc(bookmarkCount),
                        cb.desc(courseJoin.get("stdgScor")),
                        cb.desc(root.get("id"))
                );
            }
            return cb.conjunction();
        };
    }

    /**
     * 최신순 Criteria 정렬 — prioritizeFull과 함께 사용할 때 Pageable Sort 대신 적용합니다.
     */
    public static Specification<CourseSession> orderByLatest() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                query.orderBy(cb.desc(root.get("id")));
            }
            return cb.conjunction();
        };
    }

    /**
     * 만족도순 Criteria 정렬 — prioritizeFull과 함께 사용할 때 Pageable Sort 대신 적용합니다.
     */
    public static Specification<CourseSession> orderBySatisfaction() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                var courseJoin = root.join("course", JoinType.LEFT);
                query.orderBy(
                        cb.desc(courseJoin.get("stdgScor")),
                        cb.desc(root.get("id"))
                );
            }
            return cb.conjunction();
        };
    }

    /**
     * 취업률순 Criteria 정렬 — prioritizeFull과 함께 사용할 때 Pageable Sort 대신 적용합니다.
     */
    public static Specification<CourseSession> orderByEmploymentRate() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                query.orderBy(
                        cb.desc(root.get("employmentRate")),
                        cb.desc(root.get("id"))
                );
            }
            return cb.conjunction();
        };
    }

    /**
     * 기관 이미지·만족도·취업률·별점이 모두 있는 과정을 기존 정렬 기준 내에서 우선 노출합니다.
     * 기존 ORDER BY 앞에 완전성 우선순위(0=완전, 1=불완전)를 삽입합니다.
     * 반드시 기본 정렬 Specification을 체이닝한 뒤 마지막에 추가해야 합니다.
     */
    public static Specification<CourseSession> orderByCompleteDataFirst() {
        return (root, query, cb) -> {
            if (query == null || Long.class == query.getResultType() || long.class == query.getResultType()) {
                return cb.conjunction();
            }

            var courseJoin = root.join("course", JoinType.LEFT);
            var institutionJoin = courseJoin.join("institution", JoinType.LEFT);

            // 사용자 리뷰 존재 여부 서브쿼리
            Subquery<Long> reviewCount = query.subquery(Long.class);
            Root<Review> review = reviewCount.from(Review.class);
            reviewCount.select(cb.count(review.get("id")));
            reviewCount.where(
                    cb.and(
                            cb.equal(review.get("course").get("id"), courseJoin.get("id")),
                            cb.isNull(review.get("deletedAt"))
                    )
            );

            // 크롤링 리뷰 존재 여부 서브쿼리
            Subquery<Long> crawledReviewCount = query.subquery(Long.class);
            Root<CrawledReview> crawled = crawledReviewCount.from(CrawledReview.class);
            crawledReviewCount.select(cb.count(crawled.get("id")));
            crawledReviewCount.where(
                    cb.and(
                            cb.equal(crawled.get("course").get("id"), courseJoin.get("id")),
                            cb.isNotNull(crawled.get("rating"))
                    )
            );

            // 4가지 조건 모두 충족 시 우선순위 0, 아닐 경우 1
            var completePriority = cb.selectCase()
                    .when(
                            cb.and(
                                    cb.isNotNull(institutionJoin.get("profileImageUrl")),
                                    cb.isNotNull(courseJoin.get("stdgScor")),
                                    cb.isNotNull(root.get("employmentRate")),
                                    cb.or(
                                            cb.greaterThan(reviewCount, 0L),
                                            cb.greaterThan(crawledReviewCount, 0L)
                                    )
                            ),
                            0
                    )
                    .otherwise(1);

            List<Order> orders = new ArrayList<>();
            orders.add(cb.asc(completePriority));
            orders.addAll(query.getOrderList());
            query.orderBy(orders);

            return cb.conjunction();
        };
    }

    /**
     * 모집 마감 임박순 정렬입니다.
     * 시작 예정 과정은 시작일 오름차순으로 먼저 노출하고, 이미 시작했거나 시작일이 없는 과정은 뒤로 보냅니다.
     */
    public static Specification<CourseSession> orderByDeadlineSoon(LocalDate today) {
        return (root, query, cb) -> {
            if (today == null) {
                return null;
            }
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                Path<LocalDate> startDate = root.get("traStartDate");
                var deadlinePriority = cb.selectCase()
                        .when(cb.isNull(startDate), 2)
                        .when(cb.greaterThanOrEqualTo(startDate, today), 0)
                        .otherwise(1);

                query.orderBy(
                        cb.asc(deadlinePriority),
                        cb.asc(startDate),
                        cb.desc(root.get("id"))
                );
            }
            return cb.conjunction();
        };
    }
}
