package com.bootsignal.domain.inquiry.controller;

import com.bootsignal.domain.inquiry.dto.InquiryCreateRequest;
import com.bootsignal.domain.inquiry.dto.InquiryResponse;
import com.bootsignal.domain.inquiry.entity.InquiryStatus;
import com.bootsignal.domain.inquiry.service.InquiryService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자 문의 컨트롤러가 요청 DTO와 공통 응답 형식을 올바르게 처리하는지 검증합니다.
 */
@WebMvcTest(controllers = InquiryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InquiryController 테스트")
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InquiryService inquiryService;

    @Test
    void createMapsRequestAndReturnsCreatedInquiry() throws Exception {
        given(inquiryService.create(any(InquiryCreateRequest.class)))
            .willReturn(inquiryResponse(1L, InquiryStatus.PENDING, null));

        mockMvc.perform(post("/api/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "문의 제목",
                      "content": "문의 내용"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.inquiryId").value(1))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<InquiryCreateRequest> captor = ArgumentCaptor.forClass(InquiryCreateRequest.class);
        verify(inquiryService).create(captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("문의 제목");
        assertThat(captor.getValue().content()).isEqualTo("문의 내용");
    }

    @Test
    void getMyInquiriesReturnsPageResponse() throws Exception {
        given(inquiryService.getMyInquiries(any()))
            .willReturn(new PageResponse<>(
                List.of(inquiryResponse(1L, InquiryStatus.COMPLETED, "답변")),
                0,
                10,
                1,
                1,
                false
            ));

        mockMvc.perform(get("/api/inquiries")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].inquiryId").value(1))
            .andExpect(jsonPath("$.data.content[0].adminReply").value("답변"))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    private InquiryResponse inquiryResponse(Long id, InquiryStatus status, String adminReply) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 10, 0);
        return new InquiryResponse(
            id,
            "문의 제목",
            "문의 내용",
            status,
            adminReply,
            adminReply == null ? null : now,
            now,
            now
        );
    }
}
