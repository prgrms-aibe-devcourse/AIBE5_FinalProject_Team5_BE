package com.bootsignal.domain.review.dto;

import com.bootsignal.domain.review.entity.ReviewAttendanceType;
import com.bootsignal.domain.review.entity.ReviewCompletionStatus;
import com.bootsignal.domain.review.entity.ReviewDifficultyLevel;
import com.bootsignal.domain.review.entity.ReviewDropoutMajorReason;
import com.bootsignal.domain.review.entity.ReviewDropoutSubReason;
import com.bootsignal.domain.review.entity.ReviewEmploymentStatus;
import com.bootsignal.domain.review.entity.ReviewLearningGoal;
import com.bootsignal.domain.review.entity.ReviewPriorKnowledgeLevel;
import com.bootsignal.domain.review.entity.ReviewProgressSpeed;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 프론트의 인증 리뷰 5단계 설문 입력값을 받는 요청 DTO입니다.
 */
public record VerifiedReviewDetailRequest(
    @NotNull ReviewPriorKnowledgeLevel priorKnowledgeLevel,
    @NotNull @Min(1) @Max(120) Integer age,
    @NotNull ReviewLearningGoal learningGoal,
    @NotNull ReviewAttendanceType attendanceType,
    @NotNull @Min(1) @Max(1000) Integer cohort,

    @NotNull ReviewDifficultyLevel courseDifficulty,
    @NotNull ReviewProgressSpeed progressSpeed,
    @NotNull ReviewDifficultyLevel teamProjectDifficulty,
    @NotNull @Min(1) @Max(24) Integer avgSelfStudyHours,

    @NotNull @Min(1) @Max(5) Integer instructorDeliveryRating,
    @NotNull @Min(1) @Max(5) Integer curriculumRating,
    @JsonAlias("employmentSupportSatisfactionRating")
    @NotNull @Min(1) @Max(5) Integer employmentSupportRating,

    @NotNull @Min(1) @Max(100) Integer projectCount,
    @NotNull @Min(1) @Max(5) Integer projectAchievementRating,
    @NotNull @Min(1) @Max(5) Integer toolSupportRating,
    @NotNull @Min(1) @Max(5) Integer mentoringSatisfactionRating,

    @NotNull ReviewCompletionStatus completionStatus,
    ReviewDropoutMajorReason dropoutMajorReason,
    ReviewDropoutSubReason dropoutSubReason,
    @JsonAlias("employmentStatusIn6Months")
    ReviewEmploymentStatus employmentStatus,
    @JsonAlias("freeReview")
    @Size(max = 2000) String collaborationComment
) {
}
