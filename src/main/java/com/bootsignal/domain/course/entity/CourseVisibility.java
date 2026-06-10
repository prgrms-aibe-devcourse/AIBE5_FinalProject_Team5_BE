package com.bootsignal.domain.course.entity;

import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "course_visibility",
    uniqueConstraints = @UniqueConstraint(name = "uk_course_visibility_course_id", columnNames = "course_id")
)
public class CourseVisibility extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_course_visibility_course"))
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @Column(length = 255)
    private String reason;

    @Builder
    private CourseVisibility(Course course, CourseStatus status, String reason) {
        this.course = course;
        this.status = status;
        this.reason = reason;
    }

    public void change(CourseStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }
}
