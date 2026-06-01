package com.bootsignal.global.security;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

	private SecurityUtil() {}

	public static String getCurrentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new BootSignalException(ErrorCode.UNAUTHORIZED);
		}
		return authentication.getName();
	}
}
