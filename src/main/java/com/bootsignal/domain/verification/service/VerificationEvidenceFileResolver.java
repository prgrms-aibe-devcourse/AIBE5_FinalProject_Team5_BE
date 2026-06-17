package com.bootsignal.domain.verification.service;

import com.bootsignal.domain.verification.dto.VerificationEvidenceFile;
import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationEvidenceType;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.util.StringUtils;

/**
 * 인증 신청 엔티티에 저장된 자료 유형별 BLOB 데이터를 다운로드 DTO로 변환하는 클래스입니다.
 */
final class VerificationEvidenceFileResolver {

    private VerificationEvidenceFileResolver() {
    }

    static VerificationEvidenceFile toEvidenceFile(
        Verification verification,
        VerificationEvidenceType evidenceType
    ) {
        byte[] evidenceData = resolveData(verification, evidenceType);
        if (evidenceData == null || evidenceData.length == 0) {
            throw new BootSignalException(
                ErrorCode.VERIFICATION_EVIDENCE_INVALID,
                evidenceType.displayName() + " 정보가 올바르지 않습니다."
            );
        }

        return new VerificationEvidenceFile(
            resolveFileName(verification, evidenceType),
            resolveContentType(verification, evidenceType),
            evidenceData
        );
    }

    private static String resolveFileName(Verification verification, VerificationEvidenceType evidenceType) {
        String fileName = switch (evidenceType) {
            case JOB_TRAINING_HISTORY -> verification.getJobTrainingHistoryFileName();
            case ONLINE_COURSE_APPLICATION -> verification.getOnlineCourseApplicationFileName();
        };
        return StringUtils.hasText(fileName) ? fileName : evidenceType.defaultFileName();
    }

    private static String resolveContentType(Verification verification, VerificationEvidenceType evidenceType) {
        return switch (evidenceType) {
            case JOB_TRAINING_HISTORY -> verification.getJobTrainingHistoryContentType();
            case ONLINE_COURSE_APPLICATION -> verification.getOnlineCourseApplicationContentType();
        };
    }

    private static byte[] resolveData(Verification verification, VerificationEvidenceType evidenceType) {
        return switch (evidenceType) {
            case JOB_TRAINING_HISTORY -> verification.getJobTrainingHistoryData();
            case ONLINE_COURSE_APPLICATION -> verification.getOnlineCourseApplicationData();
        };
    }
}
