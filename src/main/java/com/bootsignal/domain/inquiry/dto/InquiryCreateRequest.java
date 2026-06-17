package com.bootsignal.domain.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자가 문의를 등록할 때 전달하는 요청 DTO입니다.
 */
public record InquiryCreateRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 5000) String content
) {
}
