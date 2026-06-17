package com.bootsignal.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 관리자가 인증 신청을 반려할 때 반려 사유를 전달하는 요청 DTO입니다.
 */
public record AdminVerificationRejectRequest(
    @NotBlank(message = "반려 사유는 필수입니다.")
    @Size(max = 1000, message = "반려 사유는 1000자 이하여야 합니다.")
    String reason
) {
}
