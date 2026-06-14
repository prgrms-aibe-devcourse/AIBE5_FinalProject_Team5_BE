package com.bootsignal.domain.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.course.dto.CourseDetailResponse;
import com.bootsignal.domain.course.dto.CourseListResponse;
import com.bootsignal.domain.course.service.CourseService;
import com.bootsignal.domain.course_session.dto.CourseSessionResponse;
import com.bootsignal.domain.course_session.service.CourseSessionService;
import com.bootsignal.global.dto.PageResponse;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@WebMvcTest(controllers = CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private CourseSessionService courseSessionService;

    @Test
    void getCoursesReturnsPageResponse() throws Exception {
        CourseListResponse course = new CourseListResponse(
                1L, "TR001", "Spring Boot Course", "Boot Camp Center",
                "01", 30, 0, BigDecimal.valueOf(4.5),
                100, 800, "SW Dev", "http://image.url"
        );
        PageResponse<CourseListResponse> response = new PageResponse<>(
                List.of(course), 0, 20, 1L, 1, false
        );

        given(courseService.getCourses(any())).willReturn(response);

        mockMvc.perform(get("/api/courses")
                        .queryParam("keyword", "Spring")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Spring Boot Course"))
                .andExpect(jsonPath("$.data.content[0].institutionName").value("Boot Camp Center"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void getCourseDetailReturnsCourseDetail() throws Exception {
        CourseDetailResponse detail = new CourseDetailResponse(
                1L, "TR001", "Spring Boot Course", "0101", "SW Dev", "Y",
                30, 0, BigDecimal.valueOf(4.5),
                100, 800, "01", "None", "Learn Spring Boot", "http://link.url",
                LocalDateTime.now(), LocalDateTime.now(), null
        );

        given(courseService.getCourseDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/api/courses/{courseId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Spring Boot Course"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void getCourseDetailReturnsNotFoundWhenCourseDoesNotExist() throws Exception {
        given(courseService.getCourseDetail(999L))
                .willThrow(new BootSignalException(ErrorCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/{courseId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
    }

    @Test
    void getSessionsReturnsList() throws Exception {
        CourseSessionResponse session = new CourseSessionResponse(
                1L, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                30, 30, 28, 28, "90.0", "95.0", "Weekdays", 30, 30, 30, BigDecimal.valueOf(93.3),
                "http://link.url", 3000000, 0, 120, 960
        );

        given(courseSessionService.getSessionsByCourseId(1L)).willReturn(List.of(session));

        mockMvc.perform(get("/api/courses/{courseId}/sessions", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].trprDegr").value(1))
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
