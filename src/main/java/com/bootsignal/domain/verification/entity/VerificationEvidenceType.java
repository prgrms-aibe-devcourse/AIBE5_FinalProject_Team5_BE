package com.bootsignal.domain.verification.entity;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Arrays;

/**
 * 인증 신청 시 제출받는 자료의 종류와 API 경로 식별자를 정의하는 enum입니다.
 */
public enum VerificationEvidenceType {

    JOB_TRAINING_HISTORY(
        "job-training-history",
        "job-training-history-evidence",
        "직업훈련 이력 자료"
    ),
    ONLINE_COURSE_APPLICATION(
        "online-course-application",
        "online-course-application-evidence",
        "온라인 수강 신청 이력 자료"
    );

    private final String pathSegment;
    private final String defaultFileName;
    private final String displayName;

    VerificationEvidenceType(String pathSegment, String defaultFileName, String displayName) {
        this.pathSegment = pathSegment;
        this.defaultFileName = defaultFileName;
        this.displayName = displayName;
    }

    public String pathSegment() {
        return pathSegment;
    }

    public String defaultFileName() {
        return defaultFileName;
    }

    public String displayName() {
        return displayName;
    }

    public static VerificationEvidenceType fromPathSegment(String pathSegment) {
        return Arrays.stream(values())
            .filter(type -> type.pathSegment.equals(pathSegment))
            .findFirst()
            .orElseThrow(() -> new BootSignalException(
                ErrorCode.BAD_REQUEST,
                "지원하지 않는 인증 자료 유형입니다."
            ));
    }
}
