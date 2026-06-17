package com.bootsignal.domain.verification.entity;

/**
 * 인증 신청의 처리 상태를 나타내며, 관리자는 PENDING 상태만 승인 또는 반려할 수 있습니다.
 */
public enum VerificationStatus {
    PENDING, APPROVED, REJECTED
}
