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
 * Refresh Token의 서버 측 상태를 관리하는 엔티티입니다.
 * 원문 토큰 대신 해시와 만료/폐기/교체 상태만 저장합니다.
 */
@Entity
@Getter
@Table(
	name = "refresh_tokens",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash")
	},
	indexes = {
		@Index(name = "idx_refresh_tokens_user_status", columnList = "user_id, revoked, replaced")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "user_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_refresh_tokens_user")
	)
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean revoked;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(nullable = false)
	private boolean replaced;

	@Column(name = "replaced_at")
	private Instant replacedAt;

	private RefreshToken(User user, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.revoked = false;
		this.replaced = false;
	}

	public static RefreshToken issue(User user, String tokenHash, Instant expiresAt) {
		return new RefreshToken(user, tokenHash, expiresAt);
	}

	public boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}

	public boolean isReusable(Instant now) {
		return !revoked && !replaced && !isExpired(now);
	}

	public void revoke(Instant revokedAt) {
		if (!revoked) {
			this.revoked = true;
			this.revokedAt = revokedAt;
		}
	}

	public void replace(Instant replacedAt) {
		this.replaced = true;
		this.replacedAt = replacedAt;
		revoke(replacedAt);
	}
}
