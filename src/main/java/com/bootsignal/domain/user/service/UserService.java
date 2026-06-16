package com.bootsignal.domain.user.service;

import com.bootsignal.domain.auth.repository.RefreshTokenRepository;
import com.bootsignal.domain.user.dto.MemberActionResponse;
import com.bootsignal.domain.user.dto.PasswordChangeRequest;
import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.user.storage.ProfileImageStorage;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로그인 사용자의 프로필 조회/수정, 비밀번호 변경, 회원 탈퇴를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private static final int MAX_NICKNAME_LENGTH = 30;

	private final UserRepository userRepository;
	private final ProfileImageStorage profileImageStorage;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokenRepository;

	public UserInfoResponse getMyInfo() {
		return UserInfoResponse.from(findActiveUser());
	}

	@Transactional
	public UserInfoResponse updateMyInfo(String nickname, MultipartFile profileImage) {
		User user = findActiveUser();
		String normalizedNickname = normalizeNickname(nickname);

		if ((profileImage == null || profileImage.isEmpty())
			&& (normalizedNickname == null || normalizedNickname.equals(user.getNickname()))) {
			return UserInfoResponse.from(user);
		}

		String nicknameForUpdate = null;
		if (normalizedNickname != null && !normalizedNickname.equals(user.getNickname())) {
			validateNicknameAvailable(user, normalizedNickname);
			nicknameForUpdate = normalizedNickname;
		}

		String profileImageUrl = null;
		if (profileImage != null && !profileImage.isEmpty()) {
			profileImageUrl = profileImageStorage.store(profileImage, user.getId());
		}

		try {
			user.updateProfile(nicknameForUpdate, profileImageUrl);
			return UserInfoResponse.from(user);
		} catch (DataIntegrityViolationException exception) {
			throw new BootSignalException(ErrorCode.DUPLICATE_NICKNAME);
		}
	}

	@Transactional
	public MemberActionResponse changePassword(PasswordChangeRequest request) {
		User user = findActiveUser();
		validatePasswordLoginAvailable(user);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BootSignalException(ErrorCode.INVALID_CURRENT_PASSWORD);
		}
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new BootSignalException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		revokeActiveRefreshTokens(user, Instant.now());
		return MemberActionResponse.success();
	}

	@Transactional
	public void deleteMyAccount() {
		User user = findActiveUser();
		user.softDelete(LocalDateTime.now());
		revokeActiveRefreshTokens(user, Instant.now());
	}

	private User findActiveUser() {
		String email = SecurityUtil.getCurrentUserEmail();

		return userRepository.findByEmail(email)
			.filter(activeUser -> !activeUser.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.USER_NOT_FOUND));
	}

	private String normalizeNickname(String nickname) {
		if (nickname == null) {
			return null;
		}

		String normalized = nickname.strip();
		if (!StringUtils.hasText(normalized)) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "닉네임은 비어 있을 수 없습니다.");
		}
		if (normalized.length() > MAX_NICKNAME_LENGTH) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "닉네임은 30자 이하여야 합니다.");
		}

		return normalized;
	}

	private void validateNicknameAvailable(User user, String nickname) {
		if (nickname == null) {
			return;
		}
		if (userRepository.existsByNicknameAndIdNot(nickname, user.getId())) {
			throw new BootSignalException(ErrorCode.DUPLICATE_NICKNAME);
		}
	}

	private void validatePasswordLoginAvailable(User user) {
		if (user.getProvider() != AuthProvider.LOCAL || !user.hasPassword()) {
			throw new BootSignalException(ErrorCode.SOCIAL_LOGIN_REQUIRED);
		}
	}

	private void revokeActiveRefreshTokens(User user, Instant now) {
		refreshTokenRepository.findAllByUserAndRevokedFalseAndReplacedFalse(user)
			.forEach(activeToken -> activeToken.revoke(now));
	}
}
