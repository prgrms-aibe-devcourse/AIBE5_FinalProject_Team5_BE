package com.bootsignal.domain.verification.dto;

import jakarta.validation.constraints.Size;

/**
 * 관리자가 인증 신청을 승인할 때 선택적으로 남기는 메모 요청 DTO입니다.
 */
public record AdminVerificationApproveRequest(
    @Size(max = 1000, message = "관리자 메모는 1000자 이하여야 합니다.")
    String memo
) {
}
