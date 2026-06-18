package com.bootsignal.domain.course_session.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.course_session.dto.CourseSessionDetailResponse;
import com.bootsignal.domain.course_session.service.CourseSessionService;
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

@WebMvcTest(controllers = CourseSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseSessionService courseSessionService;

    @Test
    void getCourseSessionDetailReturnsDetail() throws Exception {
        CourseSessionDetailResponse detail = new CourseSessionDetailResponse(
                1L, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                30, 30, 28, 28, "90.0", "95.0", "Weekdays", 30, 30, 30, BigDecimal.valueOf(93.3),
                "http://link.url", 10L, "TR001", "Spring Boot Course", "Boot Camp Center",
                "http://center.url", "0101", "SW Dev", "Y", 3000000, 0, BigDecimal.valueOf(4.5),
                120, 960, "01", "None", "Learn Spring Boot",
                LocalDateTime.now(), LocalDateTime.now(), null
        );

        given(courseSessionService.getCourseSessionDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/api/course-sessions/{courseSessionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.trprDegr").value(1))
                .andExpect(jsonPath("$.data.courseId").value(10))
                .andExpect(jsonPath("$.data.title").value("Spring Boot Course"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void getCourseSessionDetailReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        given(courseSessionService.getCourseSessionDetail(999L))
                .willThrow(new BootSignalException(ErrorCode.COURSE_SESSION_NOT_FOUND));

        mockMvc.perform(get("/api/course-sessions/{courseSessionId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COURSE_SESSION_NOT_FOUND"));
    }
}
