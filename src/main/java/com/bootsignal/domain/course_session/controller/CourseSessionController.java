package com.bootsignal.domain.course_session.controller;

import com.bootsignal.domain.course_session.dto.CourseSessionDetailResponse;
import com.bootsignal.domain.course_session.service.CourseSessionService;
import com.bootsignal.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-sessions")
@RequiredArgsConstructor
public class CourseSessionController {

    private final CourseSessionService courseSessionService;

    /**
     * 과정 회차(세션) 단일 조회
     * GET /api/course-sessions/{courseSessionId}
     */
    @GetMapping("/{courseSessionId}")
    public ApiResponse<CourseSessionDetailResponse> getCourseSessionDetail(
            @PathVariable Long courseSessionId) {
        return ApiResponse.success(courseSessionService.getCourseSessionDetail(courseSessionId));
    }
}
