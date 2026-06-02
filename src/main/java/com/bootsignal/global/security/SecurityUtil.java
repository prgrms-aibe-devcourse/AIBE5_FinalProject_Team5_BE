package com.bootsignal.global.security;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

	private SecurityUtil() {
	}

	public static String getCurrentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED);
		}

		String email = EmailFormatValidator.normalize(authentication.getName());
		if (!EmailFormatValidator.isValid(email)) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED, "인증 정보의 이메일이 올바르지 않습니다.");
		}
		return email;
	}
}
