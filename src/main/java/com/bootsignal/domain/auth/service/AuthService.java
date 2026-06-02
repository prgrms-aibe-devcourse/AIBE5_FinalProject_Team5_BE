package com.bootsignal.domain.auth.service;

import com.bootsignal.domain.auth.dto.LoginRequest;
import com.bootsignal.domain.auth.dto.LoginResponse;
import com.bootsignal.domain.auth.dto.SignupRequest;
import com.bootsignal.domain.auth.dto.SignupResponse;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

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
