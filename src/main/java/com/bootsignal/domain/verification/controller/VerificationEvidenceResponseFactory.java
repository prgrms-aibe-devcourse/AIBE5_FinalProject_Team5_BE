package com.bootsignal.domain.verification.controller;

import com.bootsignal.domain.verification.dto.VerificationEvidenceFile;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * DB에서 읽은 인증 자료 파일을 HTTP 다운로드 응답으로 변환하는 컨트롤러 전용 헬퍼입니다.
 */
final class VerificationEvidenceResponseFactory {

    private VerificationEvidenceResponseFactory() {
    }

    static ResponseEntity<ByteArrayResource> toResponse(VerificationEvidenceFile evidenceFile) {
        ByteArrayResource resource = new ByteArrayResource(evidenceFile.data());
        return ResponseEntity.ok()
            .contentType(resolveMediaType(evidenceFile.contentType()))
            .contentLength(evidenceFile.data().length)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(evidenceFile.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .body(resource);
    }

    private static MediaType resolveMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
