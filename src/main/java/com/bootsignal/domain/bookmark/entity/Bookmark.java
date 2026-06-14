package com.bootsignal.domain.bookmark.entity;

import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "bookmark",
	uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_session_id"})
)
public class Bookmark extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_session_id", nullable = false)
	private CourseSession courseSession;

	@Column(nullable = false)
	private LocalDate startDate; // 과정 시작 일자

	@Column(nullable = false)
	private LocalDate endDate; // 과정 종료 일자

	@Builder
	private Bookmark(User user, CourseSession courseSession, LocalDate startDate, LocalDate endDate) {
		this.user = user;
		this.courseSession = courseSession;
		this.startDate = startDate;
		this.endDate = endDate;
	}
}
