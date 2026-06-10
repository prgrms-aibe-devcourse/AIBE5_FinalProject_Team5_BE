package com.bootsignal.domain.user.controller;

import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/** 내 정보 조회  **/
	@GetMapping("/me")
	public UserInfoResponse getMyInfo() {
		return userService.getMyInfo();
	}
}
