package com.bootsignal.domain.course.controller;

import com.bootsignal.domain.course.dto.CourseDetailResponse;
import com.bootsignal.domain.course.dto.CourseListRequest;
import com.bootsignal.domain.course.dto.CourseListResponse;
import com.bootsignal.domain.course.service.CourseService;
import com.bootsignal.domain.course_session.dto.CourseSessionResponse;
import com.bootsignal.domain.course_session.service.CourseSessionService;
import com.bootsignal.global.dto.PageResponse;
import com.bootsignal.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 과정 목록, 과정 상세, 과정 회차 목록을 외부에 제공하는 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseSessionService courseSessionService;

    /**
     * 과정 회차(세션) 목록 조회 (검색 + 필터 + 페이징)
     * GET /api/courses?keyword=Java&trngAreaCd=11&fieldCategory=AI&priceRange=BELOW_30&durationFilter=WITHIN_3_MONTHS&sort=satisfaction&page=0&size=20
     * GET /api/courses?sort=popular&page=0&size=10
     *
     * keyword        : 훈련과정명, 훈련기관명 검색 (부분 일치)
     * trngAreaCd     : 지역 대분류 코드 앞 2자리 (예: 11=서울, 26=부산) — 전방 일치
     * fieldCategory  : 분야 카테고리 (AI / SECURITY / BIG_DATA / CLOUD / UI_UX / VR / APP_SW / OTHERS)
     * priceRange     : 본인 부담금 가격 필터 (BELOW_30: 30만원 이하 / BELOW_45: 45만원 이하 / BELOW_60: 60만원 이하)
     * durationFilter : 기간 필터 (총 훈련일수 기준) — WITHIN_3_MONTHS(≤90일) / WITHIN_6_MONTHS(91~180일) / OVER_6_MONTHS(>180일)
     * sort           : latest / popular / satisfaction / employmentRate / deadline
     *                  popular은 아직 시작하지 않은 과정만 북마크 수 내림차순으로 조회
     * page           : 페이지 번호 (0부터 시작, 기본값 0)
     * size           : 페이지 크기 (기본값 20, 최대 100)
     */
    @GetMapping
    public ApiResponse<PageResponse<CourseListResponse>> getCourses(
            @ModelAttribute @Valid CourseListRequest request) {
        return ApiResponse.success(courseService.getCourses(request));
    }

    /**
     * 과정 상세 조회 (institution 포함)
     * GET /api/courses/{courseId}
     */
    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailResponse> getCourse(
            @PathVariable Long courseId) {
        return ApiResponse.success(courseService.getCourseDetail(courseId));
    }

    /**
     * 과정 회차(세션) 목록 조회
     * GET /api/courses/{courseId}/sessions
     */
    @GetMapping("/{courseId}/sessions")
    public ApiResponse<List<CourseSessionResponse>> getSessions(
            @PathVariable Long courseId) {
        return ApiResponse.success(courseSessionService.getSessionsByCourseId(courseId));
    }
}
