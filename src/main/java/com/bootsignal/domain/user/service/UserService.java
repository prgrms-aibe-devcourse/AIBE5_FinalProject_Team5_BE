package com.bootsignal.domain.user.service;

import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	/** 내 정보 조회  **/
	public UserInfoResponse getMyInfo() {
		String email = SecurityUtil.getCurrentUserEmail(); // 현재 사용자 이메일

		return userRepository.findByEmail(email) 
			.filter(user -> !user.isDeleted()) 
			.map(UserInfoResponse::from) 				   // ENTITY -> DTO
			.orElseThrow(() -> new BootSignalException(ErrorCode.USER_NOT_FOUND)); 
	}
}
