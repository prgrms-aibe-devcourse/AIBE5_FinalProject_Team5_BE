package com.bootsignal.domain.user.controller;

import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}
