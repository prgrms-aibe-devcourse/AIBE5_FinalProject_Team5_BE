package com.bootsignal.domain.verification.entity;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 과정 수료/수강 인증 신청, 관리자 처리 결과, 제출 자료를 저장하는 엔티티입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "verification",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_verification_user_course_session",
        columnNames = {"user_id", "course_session_id"}
    )
)
public class Verification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_session_id", nullable = false)
    private CourseSession courseSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    @Column(name = "job_training_history_file_name", length = 255)
    private String jobTrainingHistoryFileName;

    @Column(name = "job_training_history_content_type", length = 100)
    private String jobTrainingHistoryContentType;

    @Column(name = "job_training_history_file_size")
    private Long jobTrainingHistoryFileSize;

    @Column(name = "job_training_history_s3_key", length = 500)
    private String jobTrainingHistoryS3Key;

    @Column(name = "online_course_application_file_name", length = 255)
    private String onlineCourseApplicationFileName;

    @Column(name = "online_course_application_content_type", length = 100)
    private String onlineCourseApplicationContentType;

    @Column(name = "online_course_application_file_size")
    private Long onlineCourseApplicationFileSize;

    @Column(name = "online_course_application_s3_key", length = 500)
    private String onlineCourseApplicationS3Key;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    private Verification(
        User user,
        Course course,
        CourseSession courseSession,
        String jobTrainingHistoryFileName,
        String jobTrainingHistoryContentType,
        Long jobTrainingHistoryFileSize,
        String jobTrainingHistoryS3Key,
        String onlineCourseApplicationFileName,
        String onlineCourseApplicationContentType,
        Long onlineCourseApplicationFileSize,
        String onlineCourseApplicationS3Key
    ) {
        this.user = user;
        this.course = course;
        this.courseSession = courseSession;
        this.status = VerificationStatus.PENDING;
        this.jobTrainingHistoryFileName = jobTrainingHistoryFileName;
        this.jobTrainingHistoryContentType = jobTrainingHistoryContentType;
        this.jobTrainingHistoryFileSize = jobTrainingHistoryFileSize;
        this.jobTrainingHistoryS3Key = jobTrainingHistoryS3Key;
        this.onlineCourseApplicationFileName = onlineCourseApplicationFileName;
        this.onlineCourseApplicationContentType = onlineCourseApplicationContentType;
        this.onlineCourseApplicationFileSize = onlineCourseApplicationFileSize;
        this.onlineCourseApplicationS3Key = onlineCourseApplicationS3Key;
    }

    public void approve(User admin, String adminMemo) {
        this.status = VerificationStatus.APPROVED;
        this.processedBy = admin;
        this.processedAt = LocalDateTime.now();
        this.adminMemo = adminMemo;
        this.rejectReason = null;
    }

    public void reject(User admin, String rejectReason) {
        this.status = VerificationStatus.REJECTED;
        this.processedBy = admin;
        this.processedAt = LocalDateTime.now();
        this.rejectReason = rejectReason;
    }
}
