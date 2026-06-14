package com.bootsignal.domain.calendar.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Getter
@Table(
	name = "google_calendar_token",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_google_calendar_token_user_id",
		columnNames = "user_id"
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleCalendarToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; 

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user; // 사용자 참조

	@Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
	private String accessTokenEncrypted; // 암호화 엑세스 토큰 

	@Column(name = "refresh_token_encrypted", columnDefinition = "TEXT")
	private String refreshTokenEncrypted; // 암호화 리프레시 토큰

	@Column(nullable = false, length = 500)
	private String scope; // 접근 범위

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt; // 토큰 만료 시간

	@Column(name = "connected_at", nullable = false)
	private LocalDateTime connectedAt; // 연결 시간

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt; // 연결 해제 시간

	// 활성 상태 == revoked_at IS NULL
	public boolean isActive() {
		return revokedAt == null;
	}

	// 연결 생성 (최초 연결 시도)
	public static GoogleCalendarToken connect(
		User user,
		String accessTokenEncrypted,
		String refreshTokenEncrypted,
		String scope,
		LocalDateTime expiresAt,
		LocalDateTime connectedAt
	) {
		GoogleCalendarToken token = new GoogleCalendarToken();
		token.user = user;
		token.accessTokenEncrypted = accessTokenEncrypted;
		token.refreshTokenEncrypted = refreshTokenEncrypted;
		token.scope = scope;
		token.expiresAt = expiresAt;
		token.connectedAt = connectedAt;
		token.revokedAt = null;
		return token;
	}

	// 연결 갱신 (최초 연결 해제 이후 연결)
	public void reconnect(
		String accessTokenEncrypted,
		String refreshTokenEncrypted,
		String scope,
		LocalDateTime expiresAt,
		LocalDateTime connectedAt
	) {
		this.accessTokenEncrypted = accessTokenEncrypted;
		if (StringUtils.hasText(refreshTokenEncrypted)) {
			this.refreshTokenEncrypted = refreshTokenEncrypted;
		}
		this.scope = scope;
		this.expiresAt = expiresAt;
		this.connectedAt = connectedAt;
		this.revokedAt = null;
	}

	// 연결 해제
	public void revoke(LocalDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}
}
