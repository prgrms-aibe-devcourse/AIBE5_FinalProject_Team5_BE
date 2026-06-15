package com.bootsignal.domain.verification.dto;

import org.springframework.util.StringUtils;

/**
 * 인증 신청 응답에서 제출 자료의 파일명, 콘텐츠 타입, 크기만 노출하는 DTO입니다.
 */
public record VerificationEvidenceMetadata(
    String fileName,
    String contentType,
    Long fileSize
) {

    public static VerificationEvidenceMetadata from(String fileName, String contentType, Long fileSize) {
        if (!StringUtils.hasText(fileName) && !StringUtils.hasText(contentType) && fileSize == null) {
            return null;
        }
        return new VerificationEvidenceMetadata(fileName, contentType, fileSize);
    }
}
