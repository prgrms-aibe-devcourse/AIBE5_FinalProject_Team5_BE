package com.bootsignal.domain.verification.entity;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 과정 회차 인증 신청과 관리자 처리 결과, 업로드 증빙 파일을 저장하는 엔티티입니다.
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

    @Column(name = "evidence_file_name", length = 255)
    private String evidenceFileName;

    @Column(name = "evidence_content_type", length = 100)
    private String evidenceContentType;

    @Column(name = "evidence_file_size")
    private Long evidenceFileSize;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "evidence_data")
    private byte[] evidenceData;

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
        String evidenceFileName,
        String evidenceContentType,
        Long evidenceFileSize,
        byte[] evidenceData
    ) {
        this.user = user;
        this.course = course;
        this.courseSession = courseSession;
        this.status = VerificationStatus.PENDING;
        this.evidenceFileName = evidenceFileName;
        this.evidenceContentType = evidenceContentType;
        this.evidenceFileSize = evidenceFileSize;
        this.evidenceData = evidenceData;
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
