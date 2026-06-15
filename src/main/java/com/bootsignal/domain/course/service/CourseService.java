package com.bootsignal.domain.course.service;

import com.bootsignal.domain.code.service.FieldCategoryService;
import com.bootsignal.domain.course.dto.CourseDetailResponse;
import com.bootsignal.domain.course.dto.CourseListRequest;
import com.bootsignal.domain.course.dto.CourseListResponse;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course.repository.CourseSpecification;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final FieldCategoryService fieldCategoryService;

    /**
     * 과정 목록 조회 (검색 + 필터 + 페이징)
     */
    public PageResponse<CourseListResponse> getCourses(CourseListRequest request) {
        Specification<Course> spec = Specification.allOf(
                CourseSpecification.withKeyword(request.keyword()),
                CourseSpecification.withTrngAreaCd(request.trngAreaCd()),
                CourseSpecification.withFieldCategory(request.fieldCategory(), fieldCategoryService),
                CourseSpecification.withIsFree(request.isFree()),
                CourseSpecification.withDurationFilter(request.durationFilter())
        );

        Pageable pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Specification 적용 시에도 institution fetch join 처리
        Specification<Course> withFetch = spec.and((root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                // count 쿼리가 아닐 때만 fetch join 적용
                root.fetch("institution", jakarta.persistence.criteria.JoinType.LEFT);
            }
            return cb.conjunction();
        });

        Page<Course> coursePage = courseRepository.findAll(withFetch, pageable);

        // N+1 문제 방지. 회차(Session)들을 Bulk로 조회하여 자바 메모리 단에서 매핑
        List<Long> courseIds = coursePage.getContent().stream()
                .map(Course::getId)
                .toList();

        Map<Long, List<CourseSession>> sessionsByCourse = courseSessionRepository.findByCourse_IdIn(courseIds)
                .stream()
                .collect(Collectors.groupingBy(session -> session.getCourse().getId()));

        java.time.LocalDate today = java.time.LocalDate.now();

        Page<CourseListResponse> responsePage = coursePage.map(course -> {
            List<CourseSession> sessions = sessionsByCourse.getOrDefault(course.getId(), List.of());
            CourseSession repSession = CourseSession.findRepresentativeSession(sessions, today);
            return CourseListResponse.from(course, repSession);
        });

        return PageResponse.from(responsePage);
    }

    /**
     * 과정 상세 조회 (institution 및 대표 세션 포함)
     */
    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BootSignalException(ErrorCode.COURSE_NOT_FOUND));

        List<CourseSession> sessions = courseSessionRepository.findByCourse_IdOrderByTraStartDateAsc(courseId);
        java.time.LocalDate today = java.time.LocalDate.now();
        CourseSession repSession = CourseSession.findRepresentativeSession(sessions, today);

        return CourseDetailResponse.from(course, repSession);
    }
}
