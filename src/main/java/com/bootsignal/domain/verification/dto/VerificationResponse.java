package com.bootsignal.domain.verification.dto;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import java.time.LocalDateTime;

/**
 * 사용자와 관리자가 인증 신청 목록/상세에서 확인하는 인증 신청 응답 DTO입니다.
 */
public record VerificationResponse(
    Long verificationId,
    Long userId,
    String userNickname,
    Long courseId,
    String courseTitle,
    Long courseSessionId,
    Integer courseSessionRound,
    VerificationStatus status,
    VerificationEvidenceMetadata jobTrainingHistoryFile,
    VerificationEvidenceMetadata onlineCourseApplicationFile,
    String rejectReason,
    String adminMemo,
    Long processedById,
    String processedByNickname,
    LocalDateTime processedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static VerificationResponse from(Verification verification) {
        User processedBy = verification.getProcessedBy();
        return new VerificationResponse(
            verification.getId(),
            verification.getUser().getId(),
            verification.getUser().getNickname(),
            verification.getCourse().getId(),
            verification.getCourse().getTitle(),
            verification.getCourseSession().getId(),
            verification.getCourseSession().getTrprDegr(),
            verification.getStatus(),
            VerificationEvidenceMetadata.from(
                verification.getJobTrainingHistoryFileName(),
                verification.getJobTrainingHistoryContentType(),
                verification.getJobTrainingHistoryFileSize()
            ),
            VerificationEvidenceMetadata.from(
                verification.getOnlineCourseApplicationFileName(),
                verification.getOnlineCourseApplicationContentType(),
                verification.getOnlineCourseApplicationFileSize()
            ),
            verification.getRejectReason(),
            verification.getAdminMemo(),
            processedBy == null ? null : processedBy.getId(),
            processedBy == null ? null : processedBy.getNickname(),
            verification.getProcessedAt(),
            verification.getCreatedAt(),
            verification.getUpdatedAt()
        );
    }
}
