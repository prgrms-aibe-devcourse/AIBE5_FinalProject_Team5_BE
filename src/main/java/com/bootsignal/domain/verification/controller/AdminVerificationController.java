package com.bootsignal.domain.verification.controller;

import com.bootsignal.domain.verification.dto.AdminVerificationApproveRequest;
import com.bootsignal.domain.verification.dto.AdminVerificationRejectRequest;
import com.bootsignal.domain.verification.dto.VerificationResponse;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import com.bootsignal.domain.verification.service.AdminVerificationService;
import com.bootsignal.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자가 인증 신청 목록과 상세를 조회하고 승인 또는 반려하는 API 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/admin/verifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    @GetMapping
    public PageResponse<VerificationResponse> getList(
        @RequestParam(required = false) VerificationStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<VerificationResponse> verifications = adminVerificationService.getList(
            status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(verifications);
    }

    @GetMapping("/{verificationId}")
    public VerificationResponse get(@PathVariable Long verificationId) {
        return adminVerificationService.get(verificationId);
    }

    @GetMapping("/{verificationId}/evidence")
    public ResponseEntity<ByteArrayResource> downloadEvidence(@PathVariable Long verificationId) {
        return VerificationEvidenceResponseFactory.toResponse(adminVerificationService.getEvidenceFile(verificationId));
    }

    @PatchMapping("/{verificationId}/approve")
    public VerificationResponse approve(
        @PathVariable Long verificationId,
        @RequestBody(required = false) @Valid AdminVerificationApproveRequest request
    ) {
        String memo = request == null ? null : request.memo();
        return adminVerificationService.approve(verificationId, memo);
    }

    @PatchMapping("/{verificationId}/reject")
    public VerificationResponse reject(
        @PathVariable Long verificationId,
        @RequestBody @Valid AdminVerificationRejectRequest request
    ) {
        return adminVerificationService.reject(verificationId, request.reason());
    }
}
