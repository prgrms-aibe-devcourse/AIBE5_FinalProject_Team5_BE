package com.bootsignal.domain.course_session.repository;

import com.bootsignal.domain.code.service.FieldCategoryService;
import com.bootsignal.domain.course.dto.DurationFilter;
import com.bootsignal.domain.course.dto.PriceRange;
import com.bootsignal.domain.course.dto.FieldCategory;
import com.bootsignal.domain.course_session.entity.CourseSession;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Set;

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
}
