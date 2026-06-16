package com.bootsignal.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.auth.dto.GoogleLoginRequest;
import com.bootsignal.domain.auth.dto.KakaoLoginRequest;
import com.bootsignal.domain.auth.dto.LoginRequest;
import com.bootsignal.domain.auth.dto.LoginResponse;
import com.bootsignal.domain.auth.dto.PasswordForgotRequest;
import com.bootsignal.domain.auth.dto.PasswordForgotResponse;
import com.bootsignal.domain.auth.dto.PasswordResetRequest;
import com.bootsignal.domain.auth.dto.RefreshTokenRequest;
import com.bootsignal.domain.auth.dto.SignupRequest;
import com.bootsignal.domain.auth.dto.SignupResponse;
import com.bootsignal.domain.auth.entity.PasswordResetToken;
import com.bootsignal.domain.auth.entity.RefreshToken;
import com.bootsignal.domain.auth.oauth.GoogleTokenVerifier;
import com.bootsignal.domain.auth.oauth.GoogleUserInfo;
import com.bootsignal.domain.auth.oauth.KakaoTokenVerifier;
import com.bootsignal.domain.auth.oauth.KakaoUserInfo;
import com.bootsignal.domain.auth.repository.PasswordResetTokenRepository;
import com.bootsignal.domain.auth.repository.RefreshTokenRepository;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.config.properties.PasswordResetProperties;
import com.bootsignal.global.security.jwt.JwtRefreshTokenClaims;
import com.bootsignal.global.security.jwt.JwtTokenPair;
import com.bootsignal.global.security.jwt.JwtTokenProvider;
import com.bootsignal.global.security.jwt.RefreshTokenHasher;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * AuthService의 회원가입, 로그인, 소셜 로그인, Refresh Token 회전 및 로그아웃 로직을 검증하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenHasher refreshTokenHasher;

	@Mock
	private GoogleTokenVerifier googleTokenVerifier;

	@Mock
	private KakaoTokenVerifier kakaoTokenVerifier;

	@Mock
	private PasswordResetTokenNotifier passwordResetTokenNotifier;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
			userRepository,
			refreshTokenRepository,
			passwordResetTokenRepository,
			passwordEncoder,
			jwtTokenProvider,
			refreshTokenHasher,
			googleTokenVerifier,
			kakaoTokenVerifier,
			passwordResetTokenNotifier,
			passwordResetProperties(true)
		);
	}

	@Test
	void signupSavesUserWithEncodedPassword() {
		SignupRequest request = new SignupRequest(" User@Example.COM ", "password123", " 홍길동 ", " tester ");
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByNickname("tester")).willReturn(false);
		given(passwordEncoder.encode("password123")).willReturn("encoded-password");
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = authService.signup(request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User savedUser = captor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
		assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
		assertThat(savedUser.getName()).isEqualTo("홍길동");
		assertThat(savedUser.getNickname()).isEqualTo("tester");
		assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
		assertThat(savedUser.getProvider()).isEqualTo(AuthProvider.LOCAL);
		assertThat(savedUser.getProviderUserId()).isNull();
		assertThat(savedUser.isDeleted()).isFalse();
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.name()).isEqualTo("홍길동");
		assertThat(response.nickname()).isEqualTo("tester");
	}

	@Test
	void signupThrowsDuplicateEmailWhenEmailAlreadyExists() {
		SignupRequest request = new SignupRequest("user@example.com", "password123", "tester");
		given(userRepository.existsByEmail("user@example.com")).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.DUPLICATE_EMAIL);

		verify(passwordEncoder, never()).encode(any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void signupThrowsDuplicateNicknameWhenNicknameAlreadyExists() {
		SignupRequest request = new SignupRequest("user@example.com", "password123", "tester");
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByNickname("tester")).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

		verify(passwordEncoder, never()).encode(any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void checkEmailAvailabilityReturnsAvailableFlag() {
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByEmail("used@example.com")).willReturn(true);

		assertThat(authService.checkEmailAvailability(" User@Example.COM ").available()).isTrue();
		assertThat(authService.checkEmailAvailability("used@example.com").available()).isFalse();
	}

	@Test
	void requestPasswordResetIssuesTokenForLocalUser() {
		User user = localUser(1L);
		PasswordResetToken oldToken = PasswordResetToken.issue(
			user,
			"old-hashed-reset-token",
			Instant.now().plusSeconds(600)
		);
		given(userRepository.findByEmailForUpdate("user@example.com")).willReturn(Optional.of(user));
		given(passwordResetTokenRepository.findAllByUserAndUsedAtIsNull(user)).willReturn(List.of(oldToken));
		given(refreshTokenHasher.hash(any())).willReturn("hashed-reset-token");
		given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		PasswordForgotResponse response = authService.requestPasswordReset(
			new PasswordForgotRequest(" User@Example.COM ")
		);

		ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
		ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
		verify(passwordResetTokenRepository).save(captor.capture());
		verify(passwordResetTokenNotifier).send(any(User.class), rawTokenCaptor.capture(), any(), any(Instant.class));
		assertThat(response.accepted()).isTrue();
		assertThat(response.expiresInSeconds()).isEqualTo(1800L);
		assertThat(response.resetToken()).isEqualTo(rawTokenCaptor.getValue());
		assertThat(response.resetUrl()).contains(rawTokenCaptor.getValue());
		assertThat(rawTokenCaptor.getValue()).isNotBlank();
		assertThat(oldToken.getUsedAt()).isNotNull();
		assertThat(captor.getValue().getUser()).isEqualTo(user);
		assertThat(captor.getValue().getTokenHash()).isEqualTo("hashed-reset-token");
		assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawTokenCaptor.getValue());
	}

	@Test
	void requestPasswordResetSilentlyAcceptsMissingUser() {
		given(userRepository.findByEmailForUpdate("missing@example.com")).willReturn(Optional.empty());

		PasswordForgotResponse response = authService.requestPasswordReset(new PasswordForgotRequest("missing@example.com"));

		assertThat(response.accepted()).isTrue();
		assertThat(response.expiresInSeconds()).isEqualTo(1800L);
		assertThat(response.resetToken()).isNull();
		verify(passwordResetTokenRepository, never()).save(any());
		verify(passwordResetTokenNotifier, never()).send(any(), any(), any(), any());
	}

	@Test
	void requestPasswordResetThrowsSocialLoginRequiredForSocialUser() {
		User googleUser = googleUser(2L);
		given(userRepository.findByEmailForUpdate("user@example.com")).willReturn(Optional.of(googleUser));

		assertThatThrownBy(() -> authService.requestPasswordReset(new PasswordForgotRequest("user@example.com")))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.SOCIAL_LOGIN_REQUIRED);

		verify(passwordResetTokenRepository, never()).save(any());
	}

	@Test
	void requestPasswordResetDoesNotExposeTokenWhenResponseTokenIsDisabled() {
		AuthService service = new AuthService(
			userRepository,
			refreshTokenRepository,
			passwordResetTokenRepository,
			passwordEncoder,
			jwtTokenProvider,
			refreshTokenHasher,
			googleTokenVerifier,
			kakaoTokenVerifier,
			passwordResetTokenNotifier,
			passwordResetProperties(false)
		);
		User user = localUser(1L);
		given(userRepository.findByEmailForUpdate("user@example.com")).willReturn(Optional.of(user));
		given(passwordResetTokenRepository.findAllByUserAndUsedAtIsNull(user)).willReturn(List.of());
		given(refreshTokenHasher.hash(any())).willReturn("hashed-reset-token");

		PasswordForgotResponse response = service.requestPasswordReset(new PasswordForgotRequest("user@example.com"));

		assertThat(response.accepted()).isTrue();
		assertThat(response.resetToken()).isNull();
		assertThat(response.resetUrl()).isNull();
		verify(passwordResetTokenNotifier).send(any(User.class), any(), any(), any(Instant.class));
	}

	@Test
	void resetPasswordChangesPasswordAndConsumesToken() {
		User user = localUser(1L);
		PasswordResetToken resetToken = PasswordResetToken.issue(
			user,
			"hashed-reset-token",
			Instant.now().plusSeconds(600)
		);
		PasswordResetToken otherResetToken = PasswordResetToken.issue(
			user,
			"other-hashed-reset-token",
			Instant.now().plusSeconds(600)
		);
		given(refreshTokenHasher.hash("reset-token")).willReturn("hashed-reset-token");
		given(passwordResetTokenRepository.findByTokenHash("hashed-reset-token")).willReturn(Optional.of(resetToken));
		given(passwordResetTokenRepository.findAllByUserAndUsedAtIsNull(user)).willReturn(List.of(otherResetToken));
		given(passwordEncoder.matches("newPassword123", "encoded-password")).willReturn(false);
		given(passwordEncoder.encode("newPassword123")).willReturn("new-encoded-password");
		given(refreshTokenRepository.findAllByUserAndRevokedFalseAndReplacedFalse(user)).willReturn(List.of());

		authService.resetPassword(new PasswordResetRequest("reset-token", "newPassword123"));

		assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");
		assertThat(resetToken.getUsedAt()).isNotNull();
		assertThat(otherResetToken.getUsedAt()).isNotNull();
		verify(refreshTokenRepository).findAllByUserAndRevokedFalseAndReplacedFalse(user);
	}

	@Test
	void resetPasswordRejectsExpiredToken() {
		User user = localUser(1L);
		PasswordResetToken resetToken = PasswordResetToken.issue(
			user,
			"hashed-reset-token",
			Instant.now().minusSeconds(1)
		);
		given(refreshTokenHasher.hash("reset-token")).willReturn("hashed-reset-token");
		given(passwordResetTokenRepository.findByTokenHash("hashed-reset-token")).willReturn(Optional.of(resetToken));

		assertThatThrownBy(() -> authService.resetPassword(new PasswordResetRequest("reset-token", "newPassword123")))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);

		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void resetPasswordRejectsUsedToken() {
		User user = localUser(1L);
		PasswordResetToken resetToken = PasswordResetToken.issue(
			user,
			"hashed-reset-token",
			Instant.now().plusSeconds(600)
		);
		resetToken.use(Instant.now().minusSeconds(10));
		given(refreshTokenHasher.hash("reset-token")).willReturn("hashed-reset-token");
		given(passwordResetTokenRepository.findByTokenHash("hashed-reset-token")).willReturn(Optional.of(resetToken));

		assertThatThrownBy(() -> authService.resetPassword(new PasswordResetRequest("reset-token", "newPassword123")))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);

		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void resetPasswordRejectsSocialUserAndGuidesSocialLogin() {
		User user = googleUser(2L);
		PasswordResetToken resetToken = PasswordResetToken.issue(
			user,
			"hashed-reset-token",
			Instant.now().plusSeconds(600)
		);
		given(refreshTokenHasher.hash("reset-token")).willReturn("hashed-reset-token");
		given(passwordResetTokenRepository.findByTokenHash("hashed-reset-token")).willReturn(Optional.of(resetToken));

		assertThatThrownBy(() -> authService.resetPassword(new PasswordResetRequest("reset-token", "newPassword123")))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.SOCIAL_LOGIN_REQUIRED);

		assertThat(resetToken.getUsedAt()).isNull();
		verify(passwordEncoder, never()).matches(any(), any());
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void loginReturnsTokenResponseWhenCredentialsAreValid() {
		LoginRequest request = new LoginRequest(" User@Example.COM ", "password123");
		User user = localUser(1L);
		JwtTokenPair tokenPair = new JwtTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
		given(jwtTokenProvider.createTokenPair(user)).willReturn(tokenPair);
		given(refreshTokenHasher.hash("refresh-token")).willReturn("hashed-refresh-token");

		LoginResponse response = authService.login(request);

		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
		RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();
		assertThat(savedRefreshToken.getUser()).isEqualTo(user);
		assertThat(savedRefreshToken.getTokenHash()).isEqualTo("hashed-refresh-token");
		assertThat(savedRefreshToken.getTokenHash()).isNotEqualTo("refresh-token");
		assertThat(savedRefreshToken.isRevoked()).isFalse();
		assertThat(savedRefreshToken.isReplaced()).isFalse();
		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.nickname()).isEqualTo("tester");
		assertThat(response.role()).isEqualTo(UserRole.USER);
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.accessTokenExpiresIn()).isEqualTo(3600L);
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		assertThat(response.refreshTokenExpiresIn()).isEqualTo(1209600L);
	}

	@Test
	void loginThrowsInvalidCredentialsWhenUserDoesNotExist() {
		LoginRequest request = new LoginRequest("missing@example.com", "password123");
		given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

		verify(passwordEncoder, never()).matches(any(), any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void loginThrowsInvalidCredentialsWhenPasswordDoesNotMatch() {
		LoginRequest request = new LoginRequest("user@example.com", "password123");
		User user = localUser(1L);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password123", "encoded-password")).willReturn(false);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void loginThrowsInvalidCredentialsWhenUserIsNotLocalProvider() {
		LoginRequest request = new LoginRequest("user@example.com", "password123");
		User user = localUser(1L);
		ReflectionTestUtils.setField(user, "provider", AuthProvider.GOOGLE);
		ReflectionTestUtils.setField(user, "passwordHash", null);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

		verify(passwordEncoder, never()).matches(any(), any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void loginRejectsSocialUserEvenWhenPasswordHashExists() {
		LoginRequest request = new LoginRequest("user@example.com", "password123");
		User user = googleUser(3L);
		ReflectionTestUtils.setField(user, "passwordHash", "encoded-social-password");
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

		verify(passwordEncoder, never()).matches(any(), any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void loginThrowsInvalidCredentialsWhenUserIsDeleted() {
		LoginRequest request = new LoginRequest("user@example.com", "password123");
		User user = localUser(1L);
		ReflectionTestUtils.setField(user, "deleted", true);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

		verify(passwordEncoder, never()).matches(any(), any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void refreshRotatesRefreshTokenAndReturnsNewTokenResponse() {
		RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
		User user = localUser(1L);
		RefreshToken storedToken = RefreshToken.issue(user, "old-refresh-hash", Instant.now().plusSeconds(600));
		JwtTokenPair newTokenPair = new JwtTokenPair("Bearer", "new-access-token", 3600L, "new-refresh-token", 1209600L);
		given(jwtTokenProvider.getRefreshTokenClaims("old-refresh-token"))
			.willReturn(new JwtRefreshTokenClaims(1L, Instant.now().plusSeconds(600)));
		given(refreshTokenHasher.hash("old-refresh-token")).willReturn("old-refresh-hash");
		given(refreshTokenRepository.findByTokenHash("old-refresh-hash")).willReturn(Optional.of(storedToken));
		given(jwtTokenProvider.createTokenPair(user)).willReturn(newTokenPair);
		given(refreshTokenHasher.hash("new-refresh-token")).willReturn("new-refresh-hash");

		LoginResponse response = authService.refresh(request);

		assertThat(storedToken.isRevoked()).isTrue();
		assertThat(storedToken.isReplaced()).isTrue();
		assertThat(response.accessToken()).isEqualTo("new-access-token");
		assertThat(response.refreshToken()).isEqualTo("new-refresh-token");

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		assertThat(captor.getValue().getTokenHash()).isEqualTo("new-refresh-hash");
		assertThat(captor.getValue().getTokenHash()).isNotEqualTo("new-refresh-token");
	}

	@Test
	void refreshRejectsReusedTokenAndRevokesActiveTokens() {
		RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
		User user = localUser(1L);
		Instant now = Instant.now();
		RefreshToken reusedToken = RefreshToken.issue(user, "old-refresh-hash", now.plusSeconds(600));
		reusedToken.replace(now.minusSeconds(10));
		RefreshToken activeToken = RefreshToken.issue(user, "active-refresh-hash", now.plusSeconds(600));
		given(jwtTokenProvider.getRefreshTokenClaims("old-refresh-token"))
			.willReturn(new JwtRefreshTokenClaims(1L, now.plusSeconds(600)));
		given(refreshTokenHasher.hash("old-refresh-token")).willReturn("old-refresh-hash");
		given(refreshTokenRepository.findByTokenHash("old-refresh-hash")).willReturn(Optional.of(reusedToken));
		given(refreshTokenRepository.findAllByUserAndRevokedFalseAndReplacedFalse(user))
			.willReturn(List.of(activeToken));

		assertThatThrownBy(() -> authService.refresh(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);

		assertThat(activeToken.isRevoked()).isTrue();
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void refreshTokenLookupUsesPessimisticWriteLock() throws Exception {
		Method method = RefreshTokenRepository.class.getMethod("findByTokenHash", String.class);

		Lock lock = method.getAnnotation(Lock.class);

		assertThat(lock).isNotNull();
		assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
	}

	@Test
	void userLookupForPasswordResetUsesPessimisticWriteLock() throws Exception {
		Method method = UserRepository.class.getMethod("findByEmailForUpdate", String.class);

		Lock lock = method.getAnnotation(Lock.class);

		assertThat(lock).isNotNull();
		assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
	}

	@Test
	void passwordResetTokenCleanupLookupUsesPessimisticWriteLock() throws Exception {
		Method method = PasswordResetTokenRepository.class.getMethod("findAllByUserAndUsedAtIsNull", User.class);

		Lock lock = method.getAnnotation(Lock.class);

		assertThat(lock).isNotNull();
		assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
	}

	@Test
	void logoutRevokesRefreshToken() {
		RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
		User user = localUser(1L);
		RefreshToken storedToken = RefreshToken.issue(user, "refresh-hash", Instant.now().plusSeconds(600));
		given(jwtTokenProvider.getRefreshTokenClaims("refresh-token"))
			.willReturn(new JwtRefreshTokenClaims(1L, Instant.now().plusSeconds(600)));
		given(refreshTokenHasher.hash("refresh-token")).willReturn("refresh-hash");
		given(refreshTokenRepository.findByTokenHash("refresh-hash")).willReturn(Optional.of(storedToken));

		authService.logout(request);

		assertThat(storedToken.isRevoked()).isTrue();
		assertThat(storedToken.getRevokedAt()).isNotNull();
	}

	@Test
	void googleLoginCreatesGoogleUserAndReturnsTokenResponse() {
		GoogleLoginRequest request = new GoogleLoginRequest("id-token");
		GoogleUserInfo googleUserInfo = new GoogleUserInfo(
			"google-sub",
			"user@example.com",
			"Google User",
			"https://example.com/profile.png"
		);
		JwtTokenPair tokenPair = new JwtTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
		given(googleTokenVerifier.verify("id-token")).willReturn(googleUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
			.willReturn(Optional.empty());
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByNickname("Google User")).willReturn(false);
		given(userRepository.save(any(User.class))).willAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);
			ReflectionTestUtils.setField(savedUser, "id", 2L);
			return savedUser;
		});
		given(jwtTokenProvider.createTokenPair(any(User.class))).willReturn(tokenPair);

		LoginResponse response = authService.googleLogin(request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User savedUser = captor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
		assertThat(savedUser.getPasswordHash()).isNull();
		assertThat(savedUser.getName()).isEqualTo("Google User");
		assertThat(savedUser.getNickname()).isEqualTo("Google User");
		assertThat(savedUser.getProvider()).isEqualTo(AuthProvider.GOOGLE);
		assertThat(savedUser.getProviderUserId()).isEqualTo("google-sub");
		assertThat(savedUser.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
		assertThat(response.userId()).isEqualTo(2L);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.accessToken()).isEqualTo("access-token");
		verify(passwordEncoder, never()).encode(any());
		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void googleLoginCreatesUniqueNicknameWhenNicknameAlreadyExists() {
		GoogleLoginRequest request = new GoogleLoginRequest("id-token");
		GoogleUserInfo googleUserInfo = new GoogleUserInfo(
			"google-sub",
			"user@example.com",
			"Google User",
			null
		);
		JwtTokenPair tokenPair = new JwtTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
		given(googleTokenVerifier.verify("id-token")).willReturn(googleUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
			.willReturn(Optional.empty());
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByNickname("Google User")).willReturn(true);
		given(userRepository.existsByNickname("Google User-1")).willReturn(false);
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(jwtTokenProvider.createTokenPair(any(User.class))).willReturn(tokenPair);

		authService.googleLogin(request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getNickname()).isEqualTo("Google User-1");
	}

	@Test
	void googleLoginReturnsTokenResponseWhenGoogleUserAlreadyExists() {
		GoogleLoginRequest request = new GoogleLoginRequest("id-token");
		GoogleUserInfo googleUserInfo = new GoogleUserInfo("google-sub", "user@example.com", "Google User", null);
		User user = googleUser(3L);
		JwtTokenPair tokenPair = new JwtTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
		given(googleTokenVerifier.verify("id-token")).willReturn(googleUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
			.willReturn(Optional.of(user));
		given(jwtTokenProvider.createTokenPair(user)).willReturn(tokenPair);

		LoginResponse response = authService.googleLogin(request);

		assertThat(response.userId()).isEqualTo(3L);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.nickname()).isEqualTo("google-user");
		assertThat(response.accessToken()).isEqualTo("access-token");
		verify(userRepository, never()).save(any());
		verify(userRepository, never()).existsByNickname(any());
	}

	@Test
	void googleLoginThrowsProviderMismatchWhenEmailBelongsToLocalUser() {
		GoogleLoginRequest request = new GoogleLoginRequest("id-token");
		GoogleUserInfo googleUserInfo = new GoogleUserInfo("google-sub", "user@example.com", "Google User", null);
		given(googleTokenVerifier.verify("id-token")).willReturn(googleUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
			.willReturn(Optional.empty());
		given(userRepository.existsByEmail("user@example.com")).willReturn(true);

		assertThatThrownBy(() -> authService.googleLogin(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.OAUTH_PROVIDER_MISMATCH);

		verify(userRepository, never()).save(any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void googleLoginThrowsInvalidCredentialsWhenGoogleUserIsDeleted() {
		GoogleLoginRequest request = new GoogleLoginRequest("id-token");
		GoogleUserInfo googleUserInfo = new GoogleUserInfo("google-sub", "user@example.com", "Google User", null);
		User user = googleUser(3L);
		ReflectionTestUtils.setField(user, "deleted", true);
		given(googleTokenVerifier.verify("id-token")).willReturn(googleUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
			.willReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.googleLogin(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

		verify(userRepository, never()).save(any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void googleLoginPropagatesInvalidOauthToken() {
		GoogleLoginRequest request = new GoogleLoginRequest("invalid-token");
		given(googleTokenVerifier.verify("invalid-token"))
			.willThrow(new BootSignalException(ErrorCode.INVALID_OAUTH_TOKEN));

		assertThatThrownBy(() -> authService.googleLogin(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_OAUTH_TOKEN);

		verify(userRepository, never()).findByProviderAndProviderUserId(any(), any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void kakaoLoginCreatesKakaoUserAndReturnsTokenResponse() {
		KakaoLoginRequest request = new KakaoLoginRequest("id-token");
		KakaoUserInfo kakaoUserInfo = new KakaoUserInfo(
			"kakao-sub",
			"user@example.com",
			"Kakao User",
			"https://example.com/kakao-profile.png"
		);
		JwtTokenPair tokenPair = new JwtTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
		given(kakaoTokenVerifier.verify("id-token")).willReturn(kakaoUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-sub"))
			.willReturn(Optional.empty());
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByNickname("Kakao User")).willReturn(false);
		given(userRepository.save(any(User.class))).willAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);
			ReflectionTestUtils.setField(savedUser, "id", 4L);
			return savedUser;
		});
		given(jwtTokenProvider.createTokenPair(any(User.class))).willReturn(tokenPair);

		LoginResponse response = authService.kakaoLogin(request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User savedUser = captor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
		assertThat(savedUser.getPasswordHash()).isNull();
		assertThat(savedUser.getName()).isEqualTo("Kakao User");
		assertThat(savedUser.getNickname()).isEqualTo("Kakao User");
		assertThat(savedUser.getProvider()).isEqualTo(AuthProvider.KAKAO);
		assertThat(savedUser.getProviderUserId()).isEqualTo("kakao-sub");
		assertThat(savedUser.getProfileImageUrl()).isEqualTo("https://example.com/kakao-profile.png");
		assertThat(response.userId()).isEqualTo(4L);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.accessToken()).isEqualTo("access-token");
		verify(passwordEncoder, never()).encode(any());
		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void kakaoLoginCreatesUniqueNicknameWhenNicknameAlreadyExists() {
		KakaoLoginRequest request = new KakaoLoginRequest("id-token");
		KakaoUserInfo kakaoUserInfo = new KakaoUserInfo(
			"kakao-sub",
			"user@example.com",
			"Kakao User",
			null
		);
		JwtTokenPair tokenPair = new JwtTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
		given(kakaoTokenVerifier.verify("id-token")).willReturn(kakaoUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-sub"))
			.willReturn(Optional.empty());
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByNickname("Kakao User")).willReturn(true);
		given(userRepository.existsByNickname("Kakao User-1")).willReturn(false);
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(jwtTokenProvider.createTokenPair(any(User.class))).willReturn(tokenPair);

		authService.kakaoLogin(request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getNickname()).isEqualTo("Kakao User-1");
	}

	@Test
	void kakaoLoginReturnsTokenResponseWhenKakaoUserAlreadyExists() {
		KakaoLoginRequest request = new KakaoLoginRequest("id-token");
		KakaoUserInfo kakaoUserInfo = new KakaoUserInfo("kakao-sub", "user@example.com", "Kakao User", null);
		User user = kakaoUser(5L);
		JwtTokenPair tokenPair = new JwtTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
		given(kakaoTokenVerifier.verify("id-token")).willReturn(kakaoUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-sub"))
			.willReturn(Optional.of(user));
		given(jwtTokenProvider.createTokenPair(user)).willReturn(tokenPair);

		LoginResponse response = authService.kakaoLogin(request);

		assertThat(response.userId()).isEqualTo(5L);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.nickname()).isEqualTo("kakao-user");
		assertThat(response.accessToken()).isEqualTo("access-token");
		verify(userRepository, never()).save(any());
		verify(userRepository, never()).existsByNickname(any());
	}

	@Test
	void kakaoLoginThrowsProviderMismatchWhenEmailBelongsToLocalUser() {
		KakaoLoginRequest request = new KakaoLoginRequest("id-token");
		KakaoUserInfo kakaoUserInfo = new KakaoUserInfo("kakao-sub", "user@example.com", "Kakao User", null);
		given(kakaoTokenVerifier.verify("id-token")).willReturn(kakaoUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-sub"))
			.willReturn(Optional.empty());
		given(userRepository.existsByEmail("user@example.com")).willReturn(true);

		assertThatThrownBy(() -> authService.kakaoLogin(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.OAUTH_PROVIDER_MISMATCH);

		verify(userRepository, never()).save(any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void kakaoLoginThrowsInvalidCredentialsWhenKakaoUserIsDeleted() {
		KakaoLoginRequest request = new KakaoLoginRequest("id-token");
		KakaoUserInfo kakaoUserInfo = new KakaoUserInfo("kakao-sub", "user@example.com", "Kakao User", null);
		User user = kakaoUser(5L);
		ReflectionTestUtils.setField(user, "deleted", true);
		given(kakaoTokenVerifier.verify("id-token")).willReturn(kakaoUserInfo);
		given(userRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-sub"))
			.willReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.kakaoLogin(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

		verify(userRepository, never()).save(any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	@Test
	void kakaoLoginPropagatesInvalidOauthToken() {
		KakaoLoginRequest request = new KakaoLoginRequest("invalid-token");
		given(kakaoTokenVerifier.verify("invalid-token"))
			.willThrow(new BootSignalException(ErrorCode.INVALID_OAUTH_TOKEN));

		assertThatThrownBy(() -> authService.kakaoLogin(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_OAUTH_TOKEN);

		verify(userRepository, never()).findByProviderAndProviderUserId(any(), any());
		verify(jwtTokenProvider, never()).createTokenPair(any());
	}

	private User localUser(Long id) {
		User user = User.signupLocal("user@example.com", "encoded-password", "tester");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private User googleUser(Long id) {
		User user = User.signupGoogle(
			"user@example.com",
			"google-sub",
			"Google User",
			"google-user",
			"https://example.com/profile.png"
		);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private User kakaoUser(Long id) {
		User user = User.signupKakao(
			"user@example.com",
			"kakao-sub",
			"Kakao User",
			"kakao-user",
			"https://example.com/kakao-profile.png"
		);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private PasswordResetProperties passwordResetProperties(boolean responseTokenEnabled) {
		return new PasswordResetProperties(
			1800L,
			responseTokenEnabled,
			"http://localhost:5173/reset-password?token={token}",
			new PasswordResetProperties.Mail(false, "no-reply@bootsignal.com", "비밀번호 재설정")
		);
	}
}
