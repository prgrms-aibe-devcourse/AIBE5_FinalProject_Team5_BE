package com.bootsignal.domain.review.dto;

import com.bootsignal.domain.review.entity.ReviewDropoutMajorReason;
import com.bootsignal.domain.review.entity.ReviewDropoutSubReason;
import com.bootsignal.domain.review.entity.ReviewEmploymentStatus;
import com.bootsignal.domain.review.entity.ReviewVerifiedDetail;

/**
 * 인증 리뷰 상세 설문 응답을 프론트 표시용 라벨 중심으로 내려주는 DTO입니다.
 */
public record VerifiedReviewDetailResponse(
    String priorKnowledgeLevel,
    Integer age,
    String learningGoal,
    String attendanceType,
    Integer cohort,
    String courseDifficulty,
    String progressSpeed,
    String teamProjectDifficulty,
    Integer avgSelfStudyHours,
    Integer instructorDeliveryRating,
    Integer curriculumRating,
    Integer employmentSupportSatisfactionRating,
    Integer projectCount,
    Integer projectAchievementRating,
    Integer toolSupportRating,
    Integer mentoringSatisfactionRating,
    String completionStatus,
    String dropoutMajorReason,
    String dropoutSubReason,
    String employmentStatusIn6Months,
    String freeReview
) {
    public static VerifiedReviewDetailResponse from(ReviewVerifiedDetail detail) {
        if (detail == null) {
            return null;
        }
        return new VerifiedReviewDetailResponse(
            detail.getPriorKnowledgeLevel().label(),
            detail.getAge(),
            detail.getLearningGoal().label(),
            detail.getAttendanceType().label(),
            detail.getCohort(),
            detail.getCourseDifficulty().label(),
            detail.getProgressSpeed().label(),
            detail.getTeamProjectDifficulty().label(),
            detail.getAvgSelfStudyHours(),
            detail.getInstructorDeliveryRating(),
            detail.getCurriculumRating(),
            detail.getEmploymentSupportSatisfactionRating(),
            detail.getProjectCount(),
            detail.getProjectAchievementRating(),
            detail.getToolSupportRating(),
            detail.getMentoringSatisfactionRating(),
            detail.getCompletionStatus().label(),
            label(detail.getDropoutMajorReason()),
            label(detail.getDropoutSubReason()),
            label(detail.getEmploymentStatusIn6Months()),
            detail.getFreeReview()
        );
    }

    private static String label(ReviewDropoutMajorReason reason) {
        return reason == null ? null : reason.label();
    }

    private static String label(ReviewDropoutSubReason reason) {
        return reason == null ? null : reason.label();
    }

    private static String label(ReviewEmploymentStatus status) {
        return status == null ? null : status.label();
    }
}
