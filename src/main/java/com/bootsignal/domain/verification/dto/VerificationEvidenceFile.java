package com.bootsignal.domain.verification.dto;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * DB에 저장된 인증 자료 파일을 다운로드 응답으로 변환하기 위한 내부 DTO입니다.
 */
public record VerificationEvidenceFile(
    String fileName,
    String contentType,
    byte[] data
) {

    public VerificationEvidenceFile {
        fileName = StringUtils.hasText(fileName) ? fileName : "verification-evidence";
        contentType = StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
