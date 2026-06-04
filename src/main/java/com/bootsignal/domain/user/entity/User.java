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
		@UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", length = 255)
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
		String profileImageUrl
	) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.nickname = nickname;
		this.role = UserRole.USER;
		this.provider = provider;
		this.profileImageUrl = profileImageUrl;
		this.deleted = false;
	}

	public static User signupLocal(String email, String encodedPassword, String nickname) {
		// 기존 users 스키마의 필수 name 컬럼은 닉네임과 같은 값으로 저장한다.
		return new User(email, encodedPassword, nickname, nickname, AuthProvider.LOCAL, null);
	}

	public static User signupGoogle(String email, String name, String nickname, String profileImageUrl) {
		// 구글 계정은 비밀번호 없이 OAuth 제공자 정보로 가입한다.
		return new User(email, null, name, nickname, AuthProvider.GOOGLE, profileImageUrl);
	}
}
