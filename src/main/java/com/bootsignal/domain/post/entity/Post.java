package com.bootsignal.domain.post.entity;

import com.bootsignal.domain.course.entity.Course;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class Post extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id")
	private Course course;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostType postType;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false)
	private boolean isValid = true;

	private LocalDateTime deletedAt;

	@Builder
	private Post(User user, Course course, PostType postType, String title, String content) {
		this.user = user;
		this.course = course;
		this.postType = postType;

		this.title = title;
		this.content = content;
	}

	public void update(String title, String content) {
		if (title != null)
			this.title = title;
		if (content != null)
			this.content = content;

	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
		this.isValid = false;
	}
}
