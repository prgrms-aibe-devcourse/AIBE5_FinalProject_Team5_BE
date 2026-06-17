package com.bootsignal.domain.notice.controller;

import com.bootsignal.domain.notice.dto.NoticeResponse;
import com.bootsignal.domain.notice.service.NoticeService;
import com.bootsignal.global.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공개 공지 컨트롤러가 고객센터 공지 목록과 상세 응답을 반환하는지 검증합니다.
 */
@WebMvcTest(controllers = NoticeController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NoticeController 테스트")
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeService noticeService;

    @Test
    void getListReturnsPublicNoticePage() throws Exception {
        given(noticeService.getList(any()))
            .willReturn(new PageResponse<>(
                List.of(noticeResponse(1L)),
                0,
                10,
                1,
                1,
                false
            ));

        mockMvc.perform(get("/api/notices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].noticeId").value(1))
            .andExpect(jsonPath("$.data.content[0].title").value("공지 제목"));
    }

    @Test
    void getReturnsPublicNoticeDetail() throws Exception {
        given(noticeService.get(eq(1L))).willReturn(noticeResponse(1L));

        mockMvc.perform(get("/api/notices/{noticeId}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.noticeId").value(1))
            .andExpect(jsonPath("$.data.content").value("공지 내용"));
    }

    private NoticeResponse noticeResponse(Long id) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 10, 0);
        return new NoticeResponse(id, "공지 제목", "공지 내용", now, now);
    }
}
