package com.bootsignal.domain.auth.service;

import com.bootsignal.domain.auth.dto.AuthActionResponse;
import com.bootsignal.domain.auth.dto.EmailAvailabilityResponse;
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
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.jwt.JwtRefreshTokenClaims;
import com.bootsignal.global.security.jwt.JwtTokenProvider;
import com.bootsignal.global.security.jwt.JwtTokenPair;
import com.bootsignal.global.security.jwt.RefreshTokenHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 회원가입, 로그인, 소셜 로그인과 Refresh Token 회전/폐기를 담당하는 인증 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private static final int MAX_NICKNAME_LENGTH = 30;
	private static final int MAX_NICKNAME_RETRY_COUNT = 100;
	private static final int PASSWORD_RESET_TOKEN_BYTES = 32;
	private static final long PASSWORD_RESET_TOKEN_VALIDITY_SECONDS = 30 * 60L;

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenHasher refreshTokenHasher;
	private final GoogleTokenVerifier googleTokenVerifier;
	private final KakaoTokenVerifier kakaoTokenVerifier;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.email());
		String nickname = normalizeRequiredText(request.nickname(), "닉네임은 필수입니다.");
		String name = normalizeName(request.name(), nickname);
		if (userRepository.existsByEmail(email)) {
			throw new BootSignalException(ErrorCode.DUPLICATE_EMAIL);
		}
		if (userRepository.existsByNickname(nickname)) {
			throw new BootSignalException(ErrorCode.DUPLICATE_NICKNAME);
		}

		User user = User.signupLocal(
			email,
			passwordEncoder.encode(request.password()),
			name,
			nickname
		);

		try {
			return SignupResponse.from(userRepository.save(user));
		} catch (DataIntegrityViolationException exception) {
			throw new BootSignalException(resolveDuplicateErrorCode(exception));
		}
	}

	public EmailAvailabilityResponse checkEmailAvailability(String email) {
		String normalizedEmail = normalizeEmail(email);
		return new EmailAvailabilityResponse(normalizedEmail, !userRepository.existsByEmail(normalizedEmail));
	}

	@Transactional
	public PasswordForgotResponse requestPasswordReset(PasswordForgotRequest request) {
		String email = normalizeEmail(request.email());
		return userRepository.findByEmail(email)
			.filter(this::isActiveUser)
			.map(user -> {
				validateLocalPasswordAccount(user);
				return issuePasswordResetToken(user);
			})
			.orElseGet(PasswordForgotResponse::acceptedWithoutToken);
	}

	@Transactional
	public AuthActionResponse resetPassword(PasswordResetRequest request) {
		Instant now = Instant.now();
		PasswordResetToken resetToken = findPasswordResetToken(request.token());

		if (resetToken.isExpired(now)) {
			throw new BootSignalException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
		}
		if (resetToken.isUsed()) {
			throw new BootSignalException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
		}

		User user = resetToken.getUser();
		if (user.isDeleted()) {
			throw new BootSignalException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
		}
		validateLocalPasswordAccount(user);
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new BootSignalException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		resetToken.use(now);
		revokeActiveRefreshTokens(user, now);
		return AuthActionResponse.success();
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
			.filter(this::isLocalActiveUser)
			.orElseThrow(this::invalidLoginCredentials);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidLoginCredentials();
		}

		return issueLoginResponse(user);
	}

	@Transactional
	public LoginResponse googleLogin(GoogleLoginRequest request) {
		GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(request.idToken());
		User user = userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleUserInfo.subject())
			.map(this::validateGoogleLoginUser)
			.orElseGet(() -> signupGoogleUser(googleUserInfo));

		return issueLoginResponse(user);
	}

	@Transactional
	public LoginResponse kakaoLogin(KakaoLoginRequest request) {
		KakaoUserInfo kakaoUserInfo = kakaoTokenVerifier.verify(request.idToken());
		User user = userRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, kakaoUserInfo.subject())
			.map(this::validateKakaoLoginUser)
			.orElseGet(() -> signupKakaoUser(kakaoUserInfo));

		return issueLoginResponse(user);
	}

	@Transactional
	public LoginResponse refresh(RefreshTokenRequest request) {
		Instant now = Instant.now();
		JwtRefreshTokenClaims claims = jwtTokenProvider.getRefreshTokenClaims(request.refreshToken());
		RefreshToken storedToken = findStoredRefreshToken(request.refreshToken());
		User user = validateRefreshTokenOwner(storedToken, claims);

		if (!storedToken.isReusable(now)) {
			handleRefreshTokenReuse(storedToken, now);
			throw new BootSignalException(resolveRefreshTokenStatus(storedToken, now));
		}

		storedToken.replace(now);
		return issueLoginResponse(user);
	}

	@Transactional
	public void logout(RefreshTokenRequest request) {
		Instant now = Instant.now();
		JwtRefreshTokenClaims claims = jwtTokenProvider.getRefreshTokenClaims(request.refreshToken());
		RefreshToken storedToken = findStoredRefreshToken(request.refreshToken());
		validateRefreshTokenOwner(storedToken, claims);

		if (storedToken.isExpired(now)) {
			storedToken.revoke(now);
			throw new BootSignalException(ErrorCode.REFRESH_TOKEN_EXPIRED);
		}
		if (storedToken.isRevoked() || storedToken.isReplaced()) {
			throw new BootSignalException(ErrorCode.REFRESH_TOKEN_REVOKED);
		}

		storedToken.revoke(now);
	}

	private LoginResponse issueLoginResponse(User user) {
		JwtTokenPair tokenPair = jwtTokenProvider.createTokenPair(user);
		storeRefreshToken(user, tokenPair);
		return LoginResponse.of(user, tokenPair);
	}

	private void storeRefreshToken(User user, JwtTokenPair tokenPair) {
		String refreshTokenHash = refreshTokenHasher.hash(tokenPair.refreshToken());
		Instant expiresAt = Instant.now().plusSeconds(tokenPair.refreshTokenExpiresIn());
		refreshTokenRepository.save(RefreshToken.issue(user, refreshTokenHash, expiresAt));
	}

	private RefreshToken findStoredRefreshToken(String refreshToken) {
		String refreshTokenHash = refreshTokenHasher.hash(refreshToken);
		return refreshTokenRepository.findByTokenHash(refreshTokenHash)
			.orElseThrow(() -> new BootSignalException(ErrorCode.INVALID_REFRESH_TOKEN));
	}

	private User validateRefreshTokenOwner(RefreshToken storedToken, JwtRefreshTokenClaims claims) {
		User user = storedToken.getUser();
		if (!user.getId().equals(claims.userId()) || user.isDeleted()) {
			throw new BootSignalException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		return user;
	}

	private void handleRefreshTokenReuse(RefreshToken storedToken, Instant now) {
		if (!storedToken.isExpired(now) && (storedToken.isRevoked() || storedToken.isReplaced())) {
			revokeActiveRefreshTokens(storedToken.getUser(), now);
		}
	}

	private PasswordForgotResponse issuePasswordResetToken(User user) {
		Instant now = Instant.now();
		String rawToken = generatePasswordResetToken();
		String tokenHash = refreshTokenHasher.hash(rawToken);
		Instant expiresAt = now.plusSeconds(PASSWORD_RESET_TOKEN_VALIDITY_SECONDS);
		passwordResetTokenRepository.save(PasswordResetToken.issue(user, tokenHash, expiresAt));
		return new PasswordForgotResponse(true, rawToken, expiresAt, PASSWORD_RESET_TOKEN_VALIDITY_SECONDS);
	}

	private PasswordResetToken findPasswordResetToken(String rawToken) {
		String tokenHash = refreshTokenHasher.hash(rawToken);
		return passwordResetTokenRepository.findByTokenHash(tokenHash)
			.orElseThrow(() -> new BootSignalException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
	}

	private String generatePasswordResetToken() {
		byte[] randomBytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private void revokeActiveRefreshTokens(User user, Instant now) {
		refreshTokenRepository.findAllByUserAndRevokedFalseAndReplacedFalse(user)
			.forEach(activeToken -> activeToken.revoke(now));
	}

	private ErrorCode resolveRefreshTokenStatus(RefreshToken storedToken, Instant now) {
		if (storedToken.isExpired(now)) {
			storedToken.revoke(now);
			return ErrorCode.REFRESH_TOKEN_EXPIRED;
		}
		if (storedToken.isRevoked() || storedToken.isReplaced()) {
			return ErrorCode.REFRESH_TOKEN_REUSED;
		}
		return ErrorCode.INVALID_REFRESH_TOKEN;
	}

	private User signupGoogleUser(GoogleUserInfo googleUserInfo) {
		validateEmailNotUsedByAnotherAccount(googleUserInfo.email());

		User user = User.signupGoogle(
			googleUserInfo.email(),
			googleUserInfo.subject(),
			googleUserInfo.name(),
			createUniqueGoogleNickname(googleUserInfo),
			googleUserInfo.pictureUrl()
		);

		try {
			return userRepository.save(user);
		} catch (DataIntegrityViolationException exception) {
			throw new BootSignalException(resolveDuplicateErrorCode(exception));
		}
	}

	private User validateGoogleLoginUser(User user) {
		if (user.isDeleted()) {
			throw invalidLoginCredentials();
		}
		if (user.getProvider() != AuthProvider.GOOGLE) {
			throw new BootSignalException(ErrorCode.OAUTH_PROVIDER_MISMATCH);
		}
		return user;
	}

	private User signupKakaoUser(KakaoUserInfo kakaoUserInfo) {
		validateEmailNotUsedByAnotherAccount(kakaoUserInfo.email());

		User user = User.signupKakao(
			kakaoUserInfo.email(),
			kakaoUserInfo.subject(),
			kakaoUserInfo.nickname(),
			createUniqueNickname(kakaoUserInfo.nickname(), kakaoUserInfo.email(), "Kakao"),
			kakaoUserInfo.pictureUrl()
		);

		try {
			return userRepository.save(user);
		} catch (DataIntegrityViolationException exception) {
			throw new BootSignalException(resolveDuplicateErrorCode(exception));
		}
	}

	private User validateKakaoLoginUser(User user) {
		if (user.isDeleted()) {
			throw invalidLoginCredentials();
		}
		if (user.getProvider() != AuthProvider.KAKAO) {
			throw new BootSignalException(ErrorCode.OAUTH_PROVIDER_MISMATCH);
		}
		return user;
	}

	private void validateEmailNotUsedByAnotherAccount(String email) {
		// 제공자 subject가 다른 계정에 같은 이메일을 자동 연결하지 않는다.
		if (userRepository.existsByEmail(email)) {
			throw new BootSignalException(ErrorCode.OAUTH_PROVIDER_MISMATCH);
		}
	}

	private String createUniqueGoogleNickname(GoogleUserInfo googleUserInfo) {
		// 구글 프로필 이름을 기본 닉네임으로 사용하고 중복이면 번호를 붙인다.
		return createUniqueNickname(googleUserInfo.name(), googleUserInfo.email(), "Google");
	}

	private String createUniqueNickname(String rawNickname, String email, String providerName) {
		// 소셜 프로필 이름을 기본 닉네임으로 사용하고 중복이면 번호를 붙인다.
		String baseNickname = normalizeNickname(rawNickname);
		if (!StringUtils.hasText(baseNickname)) {
			baseNickname = email.substring(0, email.indexOf('@'));
		}
		baseNickname = truncate(baseNickname, MAX_NICKNAME_LENGTH);

		if (!userRepository.existsByNickname(baseNickname)) {
			return baseNickname;
		}

		for (int suffix = 1; suffix <= MAX_NICKNAME_RETRY_COUNT; suffix++) {
			String candidate = appendNicknameSuffix(baseNickname, suffix);
			if (!userRepository.existsByNickname(candidate)) {
				return candidate;
			}
		}

		throw new BootSignalException(
			ErrorCode.INTERNAL_SERVER_ERROR,
			"사용 가능한 " + providerName + " 로그인 닉네임을 생성하지 못했습니다."
		);
	}

	private String normalizeNickname(String nickname) {
		return nickname == null ? "" : nickname.strip().replaceAll("\\s+", " ");
	}

	private String appendNicknameSuffix(String baseNickname, int suffix) {
		String suffixText = "-" + suffix;
		int baseMaxLength = MAX_NICKNAME_LENGTH - suffixText.length();
		return truncate(baseNickname, baseMaxLength) + suffixText;
	}

	private String truncate(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private boolean isLocalActiveUser(User user) {
		return isActiveUser(user)
			&& user.getProvider() == AuthProvider.LOCAL
			&& user.hasPassword();
	}

	private boolean isActiveUser(User user) {
		return !user.isDeleted();
	}

	private void validateLocalPasswordAccount(User user) {
		if (user.getProvider() != AuthProvider.LOCAL || !user.hasPassword()) {
			throw new BootSignalException(ErrorCode.SOCIAL_LOGIN_REQUIRED);
		}
	}

	private String normalizeRequiredText(String value, String blankMessage) {
		String normalized = value == null ? "" : value.strip();
		if (!StringUtils.hasText(normalized)) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, blankMessage);
		}
		return normalized;
	}

	private String normalizeName(String name, String fallbackNickname) {
		if (name == null) {
			return fallbackNickname;
		}
		String normalized = name.strip();
		if (!StringUtils.hasText(normalized)) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "이름은 비어 있을 수 없습니다.");
		}
		return normalized;
	}

	private BootSignalException invalidLoginCredentials() {
		return new BootSignalException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
	}

	private ErrorCode resolveDuplicateErrorCode(DataIntegrityViolationException exception) {
		String message = exception.getMostSpecificCause().getMessage();
		// 동시 요청으로 닉네임 유니크 제약이 발생한 경우를 분리한다.
		if (message != null && message.contains("uk_users_nickname")) {
			return ErrorCode.DUPLICATE_NICKNAME;
		}
		return ErrorCode.DUPLICATE_EMAIL;
	}

	private String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}
