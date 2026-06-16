package com.bootsignal.domain.review.entity;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과정 회차별 사용자 리뷰 기본 정보와 인증 리뷰 상세 설문 연관을 저장하는 엔티티입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "review",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_session_id"})
)
public class Review extends BaseEntity {

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
    private ReviewType reviewType;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ReviewVerifiedDetail verifiedDetail;

    private LocalDateTime deletedAt;

    @Builder
    private Review(User user, Course course, CourseSession courseSession,
                   ReviewType reviewType, Integer rating, String content) {
        this.user = user;
        this.course = course;
        this.courseSession = courseSession;
        this.reviewType = reviewType;
        this.rating = rating;
        this.content = content;
    }

    public void update(Integer rating, String content) {
        if (rating != null) this.rating = rating;
        if (content != null) this.content = content;
    }

    public void updateVerifiedDetail(ReviewVerifiedDetail verifiedDetail) {
        this.verifiedDetail = verifiedDetail;
        verifiedDetail.assignReview(this);
    }

    public void upgradeToVerified() {
        this.reviewType = ReviewType.VERIFIED;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
