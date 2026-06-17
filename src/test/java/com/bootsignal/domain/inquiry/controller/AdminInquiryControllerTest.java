package com.bootsignal.domain.inquiry.controller;

import com.bootsignal.domain.inquiry.dto.AdminInquiryResponse;
import com.bootsignal.domain.inquiry.dto.InquiryAnswerRequest;
import com.bootsignal.domain.inquiry.entity.InquiryStatus;
import com.bootsignal.domain.inquiry.service.AdminInquiryService;
import com.bootsignal.global.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 문의 컨트롤러가 상태 필터와 답변 등록 요청을 서비스로 전달하는지 검증합니다.
 */
@WebMvcTest(controllers = AdminInquiryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminInquiryController 테스트")
class AdminInquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminInquiryService adminInquiryService;

    @Test
    void getListAcceptsStatusFilter() throws Exception {
        given(adminInquiryService.getList(eq(InquiryStatus.PENDING), any()))
            .willReturn(new PageResponse<>(
                List.of(adminInquiryResponse(InquiryStatus.PENDING, null)),
                0,
                10,
                1,
                1,
                false
            ));

        mockMvc.perform(get("/api/admin/inquiries")
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void answerMapsRequest() throws Exception {
        given(adminInquiryService.answer(eq(100L), any(InquiryAnswerRequest.class)))
            .willReturn(adminInquiryResponse(InquiryStatus.COMPLETED, "답변입니다."));

        mockMvc.perform(patch("/api/admin/inquiries/{inquiryId}/answer", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "adminReply": "답변입니다."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.adminReply").value("답변입니다."));

        ArgumentCaptor<InquiryAnswerRequest> captor = ArgumentCaptor.forClass(InquiryAnswerRequest.class);
        verify(adminInquiryService).answer(eq(100L), captor.capture());
        assertThat(captor.getValue().adminReply()).isEqualTo("답변입니다.");
    }

    private AdminInquiryResponse adminInquiryResponse(InquiryStatus status, String adminReply) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 10, 0);
        return new AdminInquiryResponse(
            100L,
            1L,
            "사용자",
            "사용자",
            null,
            "문의 제목",
            "문의 내용",
            status,
            adminReply,
            adminReply == null ? null : 2L,
            adminReply == null ? null : "관리자",
            adminReply == null ? null : now,
            now,
            now
        );
    }
}
