package com.bootsignal.domain.crawled_review.entity;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawled_review",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "external_review_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrawledReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawledReviewSource source;

    @Column(name = "external_review_id", nullable = false)
    private String externalReviewId;

    private String reviewerNickname;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private Instant crawledAt;
}
