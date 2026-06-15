package com.bootsignal.domain.comment.entity;

import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 커뮤니티 게시글 댓글의 작성자, 게시글, 내용과 소프트 삭제 상태를 저장하는 엔티티입니다.
 */
@Entity
@Getter
@Table(
	name = "comments",
	indexes = {
		@Index(name = "idx_comments_post_created_at", columnList = "post_id, created_at"),
		@Index(name = "idx_comments_user_id", columnList = "user_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "is_valid", nullable = false)
	private boolean valid = true;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	private Comment(Post post, User user, String content) {
		this.post = post;
		this.user = user;
		this.content = content;
	}

	public void update(String content) {
		this.content = content;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
		this.valid = false;
	}
}
