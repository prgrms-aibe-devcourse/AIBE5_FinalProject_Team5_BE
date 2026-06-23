package com.bootsignal.domain.verification.service;

import com.bootsignal.domain.verification.dto.VerificationEvidenceFile;
import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationEvidenceType;
import com.bootsignal.domain.verification.storage.VerificationFileStorage;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.util.StringUtils;

/**
 * 인증 신청 엔티티에 저장된 S3 키를 이용해 파일을 다운로드하고 DTO로 변환하는 클래스입니다.
 */
final class VerificationEvidenceFileResolver {

    private VerificationEvidenceFileResolver() {
    }

    static VerificationEvidenceFile toEvidenceFile(
        Verification verification,
        VerificationEvidenceType evidenceType,
        VerificationFileStorage fileStorage
    ) {
        String s3Key = resolveS3Key(verification, evidenceType);
        if (!StringUtils.hasText(s3Key)) {
            throw new BootSignalException(
                ErrorCode.VERIFICATION_EVIDENCE_INVALID,
                evidenceType.displayName() + " 정보가 올바르지 않습니다."
            );
        }

        byte[] data = fileStorage.download(s3Key);

        return new VerificationEvidenceFile(
            resolveFileName(verification, evidenceType),
            resolveContentType(verification, evidenceType),
            data
        );
    }

    private static String resolveS3Key(Verification verification, VerificationEvidenceType evidenceType) {
        return switch (evidenceType) {
            case JOB_TRAINING_HISTORY -> verification.getJobTrainingHistoryS3Key();
            case ONLINE_COURSE_APPLICATION -> verification.getOnlineCourseApplicationS3Key();
        };
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
}
