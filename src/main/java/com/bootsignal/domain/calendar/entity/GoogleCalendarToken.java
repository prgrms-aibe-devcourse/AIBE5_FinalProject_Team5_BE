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
}
