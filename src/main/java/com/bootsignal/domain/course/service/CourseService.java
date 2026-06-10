package com.bootsignal.domain.course.service;

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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSessionRepository courseSessionRepository;

    /**
     * 과정 목록 조회 (검색 + 필터 + 페이징)
     */
    public PageResponse<CourseListResponse> getCourses(CourseListRequest request) {
        Specification<Course> spec = Specification
                .where(CourseSpecification.withKeyword(request.keyword()))
                .and(CourseSpecification.withTrngAreaCd(request.trngAreaCd()))
                .and(CourseSpecification.withNcsCd(request.ncsCd()));

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

        // N+1 문제 방지를 위해 회차(Session)들을 Bulk로 조회하여 자바 메모리 단에서 매핑
        List<Long> courseIds = coursePage.getContent().stream()
                .map(Course::getId)
                .toList();

        Map<Long, List<CourseSession>> sessionsByCourse = courseSessionRepository.findByCourse_IdIn(courseIds)
                .stream()
                .collect(Collectors.groupingBy(session -> session.getCourse().getId()));

        Page<CourseListResponse> responsePage = coursePage.map(course -> {
            List<CourseSession> sessions = sessionsByCourse.getOrDefault(course.getId(), List.of());
            java.time.LocalDate today = java.time.LocalDate.now();

            // 1. 오늘 이후(오늘 포함) 개강 예정인 세션 중 가장 빠른(가까운) 세션 선택
            // 2. 만약 없다면, 이미 개강한 과거 세션 중 가장 최근에 개강한 세션 선택
            CourseSession repSession = sessions.stream()
                    .filter(s -> s.getTraStartDate() != null && !s.getTraStartDate().isBefore(today))
                    .min(Comparator.comparing(CourseSession::getTraStartDate))
                    .orElseGet(() -> sessions.stream()
                            .filter(s -> s.getTraStartDate() != null)
                            .max(Comparator.comparing(CourseSession::getTraStartDate))
                            .orElse(sessions.stream().findFirst().orElse(null)));

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

        // 1. 오늘 이후(오늘 포함) 개강 예정인 세션 중 가장 빠른(가까운) 세션 선택
        // 2. 만약 없다면, 이미 개강한 과거 세션 중 가장 최근에 개강한 세션 선택
        CourseSession repSession = sessions.stream()
                .filter(s -> s.getTraStartDate() != null && !s.getTraStartDate().isBefore(today))
                .findFirst() // 이미 오름차순으로 정렬되어 있어 첫 번째가 가장 빠름
                .orElseGet(() -> sessions.stream()
                        .filter(s -> s.getTraStartDate() != null)
                        .max(Comparator.comparing(CourseSession::getTraStartDate))
                        .orElse(sessions.stream().findFirst().orElse(null)));

        return CourseDetailResponse.from(course, repSession);
    }
}
