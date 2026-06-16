package com.bootsignal.domain.auth.entity;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 찾기/재설정 과정에서 발급한 일회용 토큰의 해시와 사용 상태를 저장하는 엔티티입니다.
 */
@Entity
@Getter
@Table(
	name = "password_reset_tokens",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_password_reset_tokens_token_hash", columnNames = "token_hash")
	},
	indexes = {
		@Index(name = "idx_password_reset_tokens_user_expires", columnList = "user_id, expires_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "user_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_password_reset_tokens_user")
	)
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	private PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public static PasswordResetToken issue(User user, String tokenHash, Instant expiresAt) {
		return new PasswordResetToken(user, tokenHash, expiresAt);
	}

	public boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}

	public boolean isUsed() {
		return usedAt != null;
	}

	public boolean isUsable(Instant now) {
		return !isUsed() && !isExpired(now);
	}

	public void use(Instant usedAt) {
		if (this.usedAt == null) {
			this.usedAt = usedAt;
		}
	}
}
