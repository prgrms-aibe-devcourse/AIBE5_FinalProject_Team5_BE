package com.bootsignal.domain.user.service;

import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.user.storage.ProfileImageStorage;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private static final int MAX_NICKNAME_LENGTH = 30;

	private final UserRepository userRepository;
	private final ProfileImageStorage profileImageStorage;

	/** 내 정보 조회 **/
	public UserInfoResponse getMyInfo() {
		return UserInfoResponse.from(findActiveUser());
	}

	/** 내 정보 수정 **/
	  // profileImage: 루트/temp-storage/profile-images에 임시 저장 (배포 시 S3 전환 예정)
	@Transactional
	public UserInfoResponse updateMyInfo(String nickname, MultipartFile profileImage) {
		User user = findActiveUser();

		String normalizedNickname = normalizeNickname(nickname);

		// 닉네임·이미지 묶음 수정 요청 여부 검사
		if (profileImage == null || profileImage.isEmpty())  {
			if (normalizedNickname == null || normalizedNickname.equals(user.getNickname())) {
				return UserInfoResponse.from(user);
			}
		}

		// 닉네임 수정 요청 여부 검사 
		String nicknameForUpdate = null;
		if (normalizedNickname != null && !normalizedNickname.equals(user.getNickname())) {
			validateNicknameAvailable(user, normalizedNickname);
			nicknameForUpdate = normalizedNickname;
		}

		// 프로필 이미지 수정 요청 여부 검사
		String profileImageUrl = null;
		if (profileImage != null && !profileImage.isEmpty()) {
			profileImageUrl = resolveProfileImageUrl(profileImage, user.getId());
		}

		// 사용자 정보 수정 처리
		try {
			user.updateProfile(nicknameForUpdate, profileImageUrl);
			return UserInfoResponse.from(user);
		} catch (DataIntegrityViolationException exception) {
			throw new BootSignalException(ErrorCode.DUPLICATE_NICKNAME);
		}
	}

	// 프로필 이미지 URL 결정 
	private String resolveProfileImageUrl(MultipartFile profileImage, Long userId) {
		if (profileImage == null || profileImage.isEmpty()) { // 수정 요청 없는 경우
			return null;
		}
		// 프로필 이미지 저장소에 저장하고 URL 반환 (배포 시 S3 전환 예정)
		return profileImageStorage.store(profileImage, userId);
	}

	// 현재 사용자 조회
	private User findActiveUser() {
		String email = SecurityUtil.getCurrentUserEmail();
		
		return userRepository.findByEmail(email)
			.filter(activeUser -> !activeUser.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.USER_NOT_FOUND));
	}

	// 닉네임 전처리
	private String normalizeNickname(String nickname) {
		if (nickname == null) { // 수정 요청 없는 경우
			return null;
		}

		String normalized = nickname.strip(); // 앞뒤 공백 제거
		if (!StringUtils.hasText(normalized)) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "닉네임은 비어 있을 수 없습니다.");
		}
		if (normalized.length() > MAX_NICKNAME_LENGTH) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "닉네임은 30자 이하여야 합니다.");
		}

		return normalized;
	}

	// 닉네임 중복 검사
	private void validateNicknameAvailable(User user, String nickname) {
		if (nickname == null) { // 수정 요청 없는 경우
			return;
		}
		// 본인 닉네임 제외하고 중복 여부 검사 (AuthService 회원가입과 동일한 ErrorCode 사용)
		if (userRepository.existsByNicknameAndIdNot(nickname, user.getId())) {
			throw new BootSignalException(ErrorCode.DUPLICATE_NICKNAME);
		}
	}
}
