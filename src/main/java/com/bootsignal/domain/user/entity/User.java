package com.bootsignal.domain.user.entity;

import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	name = "users",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_users_email", columnNames = "email"),
		@UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname"),
		@UniqueConstraint(name = "uk_users_provider_provider_user_id", columnNames = {"provider", "provider_user_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = true, length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 50)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthProvider provider;

	@Column(name = "provider_user_id", nullable = true, length = 255)
	private String providerUserId;

	@Column(name = "profile_image_url", columnDefinition = "text")
	private String profileImageUrl;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private User(
		String email,
		String passwordHash,
		String name,
		String nickname,
		AuthProvider provider,
		String providerUserId,
		String profileImageUrl
	) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.nickname = nickname;
		this.role = UserRole.USER;
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.profileImageUrl = profileImageUrl;
		this.deleted = false;
	}

	public static User signupLocal(String email, String encodedPassword, String nickname) {
		// 기존 users 스키마의 필수 name 컬럼은 닉네임과 같은 값으로 저장한다.
		return new User(email, encodedPassword, nickname, nickname, AuthProvider.LOCAL, null, null);
	}

	public static User signupGoogle(
		String email,
		String providerUserId,
		String name,
		String nickname,
		String profileImageUrl
	) {
		// 구글 계정은 비밀번호 없이 제공자 고유 식별자로 가입한다.
		return new User(email, null, name, nickname, AuthProvider.GOOGLE, providerUserId, profileImageUrl);
	}

	public static User signupKakao(
		String email,
		String providerUserId,
		String name,
		String nickname,
		String profileImageUrl
	) {
		// 카카오 계정도 비밀번호 없이 제공자 고유 식별자로 가입한다.
		return new User(email, null, name, nickname, AuthProvider.KAKAO, providerUserId, profileImageUrl);
	}

	public void updateProfile(String nickname, String profileImageUrl) {
		if (nickname != null) { // 새 닉네임 전달
			this.nickname = nickname;
			
			// 이메일 가입 사용자는 name 컬럼도 닉네임과 동일하게 유지한다.
			if (this.provider == AuthProvider.LOCAL) {
				this.name = nickname;
			}
		}
		if (profileImageUrl != null) { // 새 프로필 이미지 전달
			this.profileImageUrl = profileImageUrl;
		}
	}
}
