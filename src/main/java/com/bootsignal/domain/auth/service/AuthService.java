package com.bootsignal.domain.auth.service;

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
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.jwt.JwtTokenProvider;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private static final int MAX_NICKNAME_LENGTH = 30;
	private static final int MAX_NICKNAME_RETRY_COUNT = 100;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final GoogleTokenVerifier googleTokenVerifier;
	private final KakaoTokenVerifier kakaoTokenVerifier;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.email());
		String nickname = request.nickname().strip();
		if (userRepository.existsByEmail(email)) {
			throw new BootSignalException(ErrorCode.DUPLICATE_EMAIL);
		}
		if (userRepository.existsByNickname(nickname)) {
			throw new BootSignalException(ErrorCode.DUPLICATE_NICKNAME);
		}

		User user = User.signupLocal(
			email,
			passwordEncoder.encode(request.password()),
			nickname
		);

		try {
			return SignupResponse.from(userRepository.save(user));
		} catch (DataIntegrityViolationException exception) {
			throw new BootSignalException(resolveDuplicateErrorCode(exception));
		}
	}

	public LoginResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
			.filter(this::isLocalActiveUser)
			.orElseThrow(this::invalidLoginCredentials);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidLoginCredentials();
		}

		return LoginResponse.of(user, jwtTokenProvider.createTokenPair(user));
	}

	@Transactional
	public LoginResponse googleLogin(GoogleLoginRequest request) {
		GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(request.idToken());
		User user = userRepository.findByEmail(googleUserInfo.email())
			.map(this::validateGoogleLoginUser)
			.orElseGet(() -> signupGoogleUser(googleUserInfo));

		return LoginResponse.of(user, jwtTokenProvider.createTokenPair(user));
	}

	@Transactional
	public LoginResponse kakaoLogin(KakaoLoginRequest request) {
		KakaoUserInfo kakaoUserInfo = kakaoTokenVerifier.verify(request.idToken());
		User user = userRepository.findByEmail(kakaoUserInfo.email())
			.map(this::validateKakaoLoginUser)
			.orElseGet(() -> signupKakaoUser(kakaoUserInfo));

		return LoginResponse.of(user, jwtTokenProvider.createTokenPair(user));
	}

	private User signupGoogleUser(GoogleUserInfo googleUserInfo) {
		User user = User.signupGoogle(
			googleUserInfo.email(),
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
		User user = User.signupKakao(
			kakaoUserInfo.email(),
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
		return !user.isDeleted()
			&& user.getProvider() == AuthProvider.LOCAL
			&& user.getPasswordHash() != null;
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
