package com.bootsignal.domain.user.controller;

import com.bootsignal.domain.user.dto.MemberActionResponse;
import com.bootsignal.domain.user.dto.PasswordChangeRequest;
import com.bootsignal.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트 요구사항의 `/api/members` 경로로 계정 관리 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final UserService userService;

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
