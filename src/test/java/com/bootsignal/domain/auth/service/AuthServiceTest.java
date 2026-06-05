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
import com.bootsignal.domain.auth.dto.SignupRequest;
import com.bootsignal.domain.auth.dto.SignupResponse;
import com.bootsignal.domain.auth.oauth.GoogleTokenVerifier;
import com.bootsignal.domain.auth.oauth.GoogleUserInfo;
import com.bootsignal.domain.auth.oauth.KakaoTokenVerifier;
import com.bootsignal.domain.auth.oauth.KakaoUserInfo;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.jwt.JwtTokenPair;
import com.bootsignal.global.security.jwt.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private GoogleTokenVerifier googleTokenVerifier;

	@Mock
	private KakaoTokenVerifier kakaoTokenVerifier;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
			userRepository,
			passwordEncoder,
			jwtTokenProvider,
			googleTokenVerifier,
			kakaoTokenVerifier
		);
	}

	@Test
	void signupSavesUserWithEncodedPassword() {
		SignupRequest request = new SignupRequest(" User@Example.COM ", "password123", " tester ");
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
		assertThat(savedUser.getName()).isEqualTo("tester");
		assertThat(savedUser.getNickname()).isEqualTo("tester");
		assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
		assertThat(savedUser.getProvider()).isEqualTo(AuthProvider.LOCAL);
		assertThat(savedUser.isDeleted()).isFalse();
		assertThat(response.email()).isEqualTo("user@example.com");
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
	void loginReturnsTokenResponseWhenCredentialsAreValid() {
		LoginRequest request = new LoginRequest(" User@Example.COM ", "password123");
		User user = localUser(1L);
		JwtTokenPair tokenPair = new JwtTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
		given(jwtTokenProvider.createTokenPair(user)).willReturn(tokenPair);

		LoginResponse response = authService.login(request);

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
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.empty());
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
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.empty());
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
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
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
		User user = localUser(1L);
		given(googleTokenVerifier.verify("id-token")).willReturn(googleUserInfo);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

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
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

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

		verify(userRepository, never()).findByEmail(any());
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
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.empty());
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
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.empty());
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
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
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
		User user = localUser(1L);
		given(kakaoTokenVerifier.verify("id-token")).willReturn(kakaoUserInfo);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

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
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

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

		verify(userRepository, never()).findByEmail(any());
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
			"Kakao User",
			"kakao-user",
			"https://example.com/kakao-profile.png"
		);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
