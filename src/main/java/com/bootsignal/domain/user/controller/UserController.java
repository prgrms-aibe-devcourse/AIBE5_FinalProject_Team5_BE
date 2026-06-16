package com.bootsignal.domain.user.controller;

import com.bootsignal.domain.user.dto.MemberActionResponse;
import com.bootsignal.domain.user.dto.PasswordChangeRequest;
import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 기존 `/api/users` 경로에서 로그인 사용자의 프로필, 비밀번호, 탈퇴 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/** 내 정보 조회 **/
	@GetMapping("/me")
	public UserInfoResponse getMyInfo() {
		return userService.getMyInfo();
	}

	/** 내 정보 수정 (multipart/form-data) **/
		// multipart/form-data 형식
		// profileImage: 루트/temp-storage/profile-images에 임시 저장 (배포 시 S3 전환 예정)	
	@PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UserInfoResponse updateMyInfo(
		@RequestParam(value = "nickname", required = false) String nickname,
		@RequestPart(value = "profileImage", required = false) MultipartFile profileImage
	) {
		return userService.updateMyInfo(nickname, profileImage);
	}

	@PatchMapping("/me/password")
	public MemberActionResponse changeMyPassword(@Valid @RequestBody PasswordChangeRequest request) {
		return userService.changePassword(request);
	}

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMyAccount() {
		userService.deleteMyAccount();
	}
}
