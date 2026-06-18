package com.bootsignal.domain.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 관리자가 문의 답변을 등록하거나 수정할 때 사용하는 요청 DTO입니다.
 */
public record InquiryAnswerRequest(
    @NotBlank @Size(max = 5000) String adminReply
) {
}
