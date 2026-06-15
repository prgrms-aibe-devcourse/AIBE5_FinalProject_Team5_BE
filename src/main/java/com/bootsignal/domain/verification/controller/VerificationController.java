package com.bootsignal.domain.verification.controller;

import com.bootsignal.domain.verification.dto.VerificationResponse;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import com.bootsignal.domain.verification.service.VerificationService;
import com.bootsignal.global.dto.PageResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로그인 사용자가 과정 회차 인증을 신청하고 본인 신청과 증빙 파일을 조회하는 API 컨트롤러입니다.
 */
@Validated
@RestController
@RequestMapping("/api/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VerificationResponse create(
        @RequestParam @NotNull(message = "과정 ID는 필수입니다.") Long courseId,
        @RequestParam @NotNull(message = "과정 회차 ID는 필수입니다.") Long courseSessionId,
        @RequestPart(value = "evidenceFile", required = false) MultipartFile evidenceFile,
        @RequestPart(value = "file", required = false) MultipartFile fallbackFile
    ) {
        return verificationService.create(courseId, courseSessionId, resolveEvidenceFile(evidenceFile, fallbackFile));
    }

    @GetMapping("/my")
    public PageResponse<VerificationResponse> getMyList(
        @RequestParam(required = false) VerificationStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<VerificationResponse> verifications = verificationService.getMyList(
            status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(verifications);
    }

    @GetMapping("/{verificationId}")
    public VerificationResponse getMyVerification(@PathVariable Long verificationId) {
        return verificationService.getMyVerification(verificationId);
    }

    @GetMapping("/{verificationId}/evidence")
    public ResponseEntity<ByteArrayResource> downloadMyEvidence(@PathVariable Long verificationId) {
        return VerificationEvidenceResponseFactory.toResponse(verificationService.getMyEvidenceFile(verificationId));
    }

    private MultipartFile resolveEvidenceFile(MultipartFile evidenceFile, MultipartFile fallbackFile) {
        return evidenceFile != null ? evidenceFile : fallbackFile;
    }
}
