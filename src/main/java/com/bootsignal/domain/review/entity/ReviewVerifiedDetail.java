package com.bootsignal.domain.review.entity;

import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 리뷰 작성 시 5단계 설문에서 입력한 상세 응답을 저장하는 엔티티입니다.
 */
@Entity
@Getter
@Table(
    name = "review_verified_detail",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_verified_detail_review_id",
        columnNames = "review_id"
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewVerifiedDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewPriorKnowledgeLevel priorKnowledgeLevel;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewLearningGoal learningGoal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewAttendanceType attendanceType;

    @Column(nullable = false)
    private Integer cohort;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewDifficultyLevel courseDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewProgressSpeed progressSpeed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewDifficultyLevel teamProjectDifficulty;

    @Column(nullable = false)
    private Integer avgSelfStudyHours;

    @Column(nullable = false)
    private Integer instructorDeliveryRating;

    @Column(nullable = false)
    private Integer curriculumRating;

    @Column(nullable = false)
    private Integer employmentSupportSatisfactionRating;

    @Column(nullable = false)
    private Integer projectCount;

    @Column(nullable = false)
    private Integer projectAchievementRating;

    @Column(nullable = false)
    private Integer toolSupportRating;

    @Column(nullable = false)
    private Integer mentoringSatisfactionRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewCompletionStatus completionStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReviewDropoutMajorReason dropoutMajorReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReviewDropoutSubReason dropoutSubReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReviewEmploymentStatus employmentStatusIn6Months;

    @Column(columnDefinition = "TEXT")
    private String freeReview;

    @Builder
    private ReviewVerifiedDetail(
        ReviewPriorKnowledgeLevel priorKnowledgeLevel,
        Integer age,
        ReviewLearningGoal learningGoal,
        ReviewAttendanceType attendanceType,
        Integer cohort,
        ReviewDifficultyLevel courseDifficulty,
        ReviewProgressSpeed progressSpeed,
        ReviewDifficultyLevel teamProjectDifficulty,
        Integer avgSelfStudyHours,
        Integer instructorDeliveryRating,
        Integer curriculumRating,
        Integer employmentSupportSatisfactionRating,
        Integer projectCount,
        Integer projectAchievementRating,
        Integer toolSupportRating,
        Integer mentoringSatisfactionRating,
        ReviewCompletionStatus completionStatus,
        ReviewDropoutMajorReason dropoutMajorReason,
        ReviewDropoutSubReason dropoutSubReason,
        ReviewEmploymentStatus employmentStatusIn6Months,
        String freeReview
    ) {
        update(
            priorKnowledgeLevel,
            age,
            learningGoal,
            attendanceType,
            cohort,
            courseDifficulty,
            progressSpeed,
            teamProjectDifficulty,
            avgSelfStudyHours,
            instructorDeliveryRating,
            curriculumRating,
            employmentSupportSatisfactionRating,
            projectCount,
            projectAchievementRating,
            toolSupportRating,
            mentoringSatisfactionRating,
            completionStatus,
            dropoutMajorReason,
            dropoutSubReason,
            employmentStatusIn6Months,
            freeReview
        );
    }

    void assignReview(Review review) {
        this.review = review;
    }

    public void update(
        ReviewPriorKnowledgeLevel priorKnowledgeLevel,
        Integer age,
        ReviewLearningGoal learningGoal,
        ReviewAttendanceType attendanceType,
        Integer cohort,
        ReviewDifficultyLevel courseDifficulty,
        ReviewProgressSpeed progressSpeed,
        ReviewDifficultyLevel teamProjectDifficulty,
        Integer avgSelfStudyHours,
        Integer instructorDeliveryRating,
        Integer curriculumRating,
        Integer employmentSupportSatisfactionRating,
        Integer projectCount,
        Integer projectAchievementRating,
        Integer toolSupportRating,
        Integer mentoringSatisfactionRating,
        ReviewCompletionStatus completionStatus,
        ReviewDropoutMajorReason dropoutMajorReason,
        ReviewDropoutSubReason dropoutSubReason,
        ReviewEmploymentStatus employmentStatusIn6Months,
        String freeReview
    ) {
        this.priorKnowledgeLevel = priorKnowledgeLevel;
        this.age = age;
        this.learningGoal = learningGoal;
        this.attendanceType = attendanceType;
        this.cohort = cohort;
        this.courseDifficulty = courseDifficulty;
        this.progressSpeed = progressSpeed;
        this.teamProjectDifficulty = teamProjectDifficulty;
        this.avgSelfStudyHours = avgSelfStudyHours;
        this.instructorDeliveryRating = instructorDeliveryRating;
        this.curriculumRating = curriculumRating;
        this.employmentSupportSatisfactionRating = employmentSupportSatisfactionRating;
        this.projectCount = projectCount;
        this.projectAchievementRating = projectAchievementRating;
        this.toolSupportRating = toolSupportRating;
        this.mentoringSatisfactionRating = mentoringSatisfactionRating;
        this.completionStatus = completionStatus;
        this.dropoutMajorReason = dropoutMajorReason;
        this.dropoutSubReason = dropoutSubReason;
        this.employmentStatusIn6Months = employmentStatusIn6Months;
        this.freeReview = freeReview;
    }
}
