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
            CourseSession repSession = findRepresentativeSession(sessions, today);
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
        CourseSession repSession = findRepresentativeSession(sessions, today);

        return CourseDetailResponse.from(course, repSession);
    }

    /**
     * 대표 기수 판별 메서드
     * 1. 오늘 이후에 개강하는 미래 세션 중 가장 개강일이 빠른 것 선택
     * 2. 미래 세션이 없다면, 과거 개강 세션 중 가장 최근에 개강한 것 선택
     * 3. 둘 다 없을 경우 첫 번째 세션을 기본값으로 선택
     */
    private CourseSession findRepresentativeSession(List<CourseSession> sessions, java.time.LocalDate today) {
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }

        CourseSession closestFutureSession = null;
        CourseSession latestPastSession = null;

        for (CourseSession session : sessions) {
            java.time.LocalDate startDate = session.getTraStartDate();
            if (startDate == null) {
                continue;
            }

            // 오늘을 포함한 미래 기수 일정
            if (!startDate.isBefore(today)) {
                if (closestFutureSession == null || startDate.isBefore(closestFutureSession.getTraStartDate())) {
                    closestFutureSession = session;
                }
            } 
            // 과거 기수 일정
            else {
                if (latestPastSession == null || startDate.isAfter(latestPastSession.getTraStartDate())) {
                    latestPastSession = session;
                }
            }
        }

        // 미래 세션 우선 반환
        if (closestFutureSession != null) {
            return closestFutureSession;
        }
        // 과거 세션 반환
        if (latestPastSession != null) {
            return latestPastSession;
        }
        // 날짜 정보가 모두 없는 등의 예외 상황 시 첫 번째 요소를 반환
        return sessions.get(0);
    }
}
