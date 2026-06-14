package com.bootsignal.domain.course.repository;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.institution.entity.Institution;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

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
     * 훈련 지역 코드 일치 필터
     */
    public static Specification<Course> withTrngAreaCd(String trngAreaCd) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(trngAreaCd)) {
                return null;
            }
            return cb.equal(root.get("trngAreaCd"), trngAreaCd);
        };
    }

    /**
     * NCS 코드 전방 일치 필터 (예: "2001" → "200101xx" 모두 포함)
     */
    public static Specification<Course> withNcsCd(String ncsCd) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(ncsCd)) {
                return null;
            }
            return cb.like(root.get("ncsCd"), ncsCd + "%");
        };
    }
}
