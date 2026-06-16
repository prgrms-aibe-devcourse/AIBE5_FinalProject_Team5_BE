package com.bootsignal.domain.course.repository;

import com.bootsignal.domain.code.service.FieldCategoryService;
import com.bootsignal.domain.course.dto.DurationFilter;
import com.bootsignal.domain.course.dto.PriceRange;
import com.bootsignal.domain.course.dto.FieldCategory;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Set;

public class CourseSpecification {

    private CourseSpecification() {
    }

    /**
     * 과정명(title) 또는 기관명(institution.institutionName) 부분 일치 검색
     */
    public static Specification<Course> withKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            var institutionJoin = root.join("institution", JoinType.LEFT);
            return cb.or(
                    cb.like(root.get("title"), "%" + keyword + "%"),
                    cb.like(institutionJoin.get("institutionName"), "%" + keyword + "%")
            );
        };
    }

    /**
     * 지역 대분류 코드 전방 일치 필터
     * 예) trngAreaCd="11" → trng_area_cd LIKE '11%' (서울 전체)
     */
    public static Specification<Course> withTrngAreaCd(String trngAreaCd) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(trngAreaCd)) {
                return null;
            }
            return cb.like(root.get("trngAreaCd"), trngAreaCd + "%");
        };
    }

    /**
     * 분야 카테고리 필터 (NCS 코드 기반 IN / NOT IN 쿼리)
     * - 일반 카테고리: ncs_cd IN (해당 카테고리 코드 목록)
     * - OTHERS: ncs_cd NOT IN (7개 카테고리 전체 코드 목록) OR ncs_cd IS NULL
     */
    public static Specification<Course> withFieldCategory(FieldCategory fieldCategory,
                                                          FieldCategoryService fieldCategoryService) {
        return (root, query, cb) -> {
            if (fieldCategory == null) {
                return null;
            }
            if (fieldCategory == FieldCategory.OTHERS) {
                Set<String> allCodes = fieldCategoryService.getAllCategorizedCodes();
                if (allCodes.isEmpty()) {
                    return null;
                }
                return cb.or(
                        root.get("ncsCd").isNull(),
                        cb.not(root.get("ncsCd").in(allCodes))
                );
            }
            Set<String> codes = fieldCategoryService.getCodesFor(fieldCategory);
            if (codes == null || codes.isEmpty()) {
                return null;
            }
            return root.get("ncsCd").in(codes);
        };
    }

    /**
     * 가격 필터 — CourseSession.selfPaymentAmount 기준 EXISTS 서브쿼리
     * isFree=true  → selfPaymentAmount = 0
     * isFree=false → selfPaymentAmount > 0
     */
    public static Specification<Course> withIsFree(Boolean isFree) {
        return (root, query, cb) -> {
            if (isFree == null || query == null) {
                return null;
            }
            Subquery<CourseSession> subquery = query.subquery(CourseSession.class);
            var sessionRoot = subquery.from(CourseSession.class);
            subquery.select(sessionRoot);

            if (isFree) {
                subquery.where(
                        cb.equal(sessionRoot.get("course"), root),
                        cb.equal(sessionRoot.get("selfPaymentAmount"), 0)
                );
            } else {
                subquery.where(
                        cb.equal(sessionRoot.get("course"), root),
                        cb.greaterThan(sessionRoot.get("selfPaymentAmount"), 0)
                );
            }
            return cb.exists(subquery);
        };
    }

    /**
     * 가격 범위 필터 — CourseSession.selfPaymentAmount 기준 EXISTS 서브쿼리
     * BELOW_30 : 0 ~ 300000 이하
     * BELOW_45 : 0 ~ 450000 이하
     * BELOW_60 : 0 ~ 600000 이하
     * priceRange 파라미터가 없으면(=null) 필터를 적용하지 않음 → 60만 초과 포함 전체 조회
     */
    public static Specification<Course> withPriceRange(PriceRange priceRange) {
        return (root, query, cb) -> {
            if (priceRange == null || query == null) {
                return null;
            }
            Subquery<CourseSession> subquery = query.subquery(CourseSession.class);
            var sessionRoot = subquery.from(CourseSession.class);
            subquery.select(sessionRoot);
            switch (priceRange) {
                case BELOW_30 -> subquery.where(
                        cb.equal(sessionRoot.get("course"), root),
                        cb.lessThanOrEqualTo(sessionRoot.get("selfPaymentAmount"), 300000)
                );
                case BELOW_45 -> subquery.where(
                        cb.equal(sessionRoot.get("course"), root),
                        cb.lessThanOrEqualTo(sessionRoot.get("selfPaymentAmount"), 450000)
                );
                case BELOW_60 -> subquery.where(
                        cb.equal(sessionRoot.get("course"), root),
                        cb.lessThanOrEqualTo(sessionRoot.get("selfPaymentAmount"), 600000)
                );
            }
            return cb.exists(subquery);
        };
    }

    /**
     * 기간 필터 — CourseSession.totalTrainingDays 기준 EXISTS 서브쿼리
     * WITHIN_3_MONTHS : totalTrainingDays <= 90
     * WITHIN_6_MONTHS : 91 <= totalTrainingDays <= 180
     * OVER_6_MONTHS   : totalTrainingDays > 180
     */
    public static Specification<Course> withDurationFilter(DurationFilter durationFilter) {
        return (root, query, cb) -> {
            if (durationFilter == null || query == null) {
                return null;
            }
            Subquery<CourseSession> subquery = query.subquery(CourseSession.class);
            var sessionRoot = subquery.from(CourseSession.class);
            subquery.select(sessionRoot);

            switch (durationFilter) {
                case WITHIN_3_MONTHS -> subquery.where(
                        cb.equal(sessionRoot.get("course"), root),
                        cb.lessThanOrEqualTo(sessionRoot.get("totalTrainingDays"), 90)
                );
                case WITHIN_6_MONTHS -> subquery.where(
                        cb.equal(sessionRoot.get("course"), root),
                        cb.greaterThan(sessionRoot.get("totalTrainingDays"), 90),
                        cb.lessThanOrEqualTo(sessionRoot.get("totalTrainingDays"), 180)
                );
                case OVER_6_MONTHS -> subquery.where(
                        cb.equal(sessionRoot.get("course"), root),
                        cb.greaterThan(sessionRoot.get("totalTrainingDays"), 180)
                );
            }
            return cb.exists(subquery);
        };
    }
}
