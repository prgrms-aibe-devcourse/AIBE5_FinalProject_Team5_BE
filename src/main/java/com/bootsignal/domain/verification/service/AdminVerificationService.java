package com.bootsignal.domain.verification.service;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.verification.dto.VerificationEvidenceFile;
import com.bootsignal.domain.verification.dto.VerificationResponse;
import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import com.bootsignal.domain.verification.repository.VerificationRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자의 인증 신청 목록 조회, 상세 조회, 승인/반려 처리를 담당하는 서비스입니다.
 */
@Service
@Transactional(readOnly = true)
public class AdminVerificationService {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;

    public AdminVerificationService(
        VerificationRepository verificationRepository,
        UserRepository userRepository
    ) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
    }

    public Page<VerificationResponse> getList(VerificationStatus status, Pageable pageable) {
        return verificationRepository.findAdminVerifications(status, pageable)
            .map(VerificationResponse::from);
    }

    public VerificationResponse get(Long verificationId) {
        return VerificationResponse.from(findVerification(verificationId));
    }

    public VerificationEvidenceFile getEvidenceFile(Long verificationId) {
        Verification verification = findVerification(verificationId);
        byte[] evidenceData = verification.getEvidenceData();
        if (evidenceData == null || evidenceData.length == 0) {
            throw new BootSignalException(ErrorCode.VERIFICATION_EVIDENCE_INVALID);
        }
        return new VerificationEvidenceFile(
            verification.getEvidenceFileName(),
            verification.getEvidenceContentType(),
            evidenceData
        );
    }

    @Transactional
    public VerificationResponse approve(Long verificationId, String memo) {
        User admin = getAuthenticatedAdmin();
        Verification verification = findVerification(verificationId);
        validatePending(verification);

        verification.approve(admin, memo);
        return VerificationResponse.from(verification);
    }

    @Transactional
    public VerificationResponse reject(Long verificationId, String reason) {
        User admin = getAuthenticatedAdmin();
        Verification verification = findVerification(verificationId);
        validatePending(verification);

        verification.reject(admin, reason);
        return VerificationResponse.from(verification);
    }

    private void validatePending(Verification verification) {
        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new BootSignalException(ErrorCode.VERIFICATION_ALREADY_PROCESSED);
        }
    }

    private Verification findVerification(Long verificationId) {
        return verificationRepository.findWithDetailsById(verificationId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    private User getAuthenticatedAdmin() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
    }
}
