package com.bootsignal.domain.auth.service;

import com.bootsignal.domain.auth.dto.SignupRequest;
import com.bootsignal.domain.auth.dto.SignupResponse;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
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
