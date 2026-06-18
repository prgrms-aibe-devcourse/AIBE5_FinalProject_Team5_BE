package com.bootsignal.domain.course_session.service;

import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.dto.CourseSessionDetailResponse;
import com.bootsignal.domain.course_session.dto.CourseSessionResponse;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseSessionService {

    private final CourseSessionRepository courseSessionRepository;
    private final CourseRepository courseRepository;

    /**
     * 과정 회차(세션) 목록 조회
     * - 과정이 존재하지 않으면 COURSE_NOT_FOUND 예외
     * - 세션이 없으면 빈 리스트 반환 (404 아님)
     * - traStartDate ASC 정렬
     */
    public List<CourseSessionResponse> getSessionsByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new BootSignalException(ErrorCode.COURSE_NOT_FOUND);
        }
        return courseSessionRepository
                .findByCourse_IdOrderByTraStartDateAsc(courseId)
                .stream()
                .map(CourseSessionResponse::from)
                .toList();
    }

    /**
     * 과정 회차(세션) 단일 조회
     * - 회차가 존재하지 않으면 COURSE_SESSION_NOT_FOUND 예외
     */
    public CourseSessionDetailResponse getCourseSessionDetail(Long courseSessionId) {
        CourseSession session = courseSessionRepository.findWithCourseAndInstitutionById(courseSessionId)
                .orElseThrow(() -> new BootSignalException(ErrorCode.COURSE_SESSION_NOT_FOUND));
        return CourseSessionDetailResponse.from(session);
    }
}
