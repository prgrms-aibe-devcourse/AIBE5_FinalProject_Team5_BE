package com.bootsignal.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.user.dto.MemberActionResponse;
import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.service.UserService;
import com.bootsignal.global.config.properties.ProfileImageProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// UserController 슬라이스 테스트 — Controller + ApiResponseAdvice만 로드, Service는 Mock
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false) // JWT 필터 비활성화 (인증은 Service Mock으로 검증)
@EnableConfigurationProperties(ProfileImageProperties.class)
@TestPropertySource(properties = {
	"app.profile-image.storage-type=local",
	"app.profile-image.local.upload-dir=temp-storage/profile-images",
	"app.profile-image.local.public-path-prefix=/local-files/profile",
	"app.profile-image.local.base-url=http://localhost:8080"
})
@DisplayName("UserController 테스트")
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	@DisplayName("GET /api/users/me — 내 정보 조회 성공 (200)")
	void getMyInfoReturnsOkResponse() throws Exception {
		// given
		given(userService.getMyInfo())
			.willReturn(new UserInfoResponse(
				1L,
				"user@example.com",
				"길동",
				UserRole.USER,
				AuthProvider.GOOGLE,
				"https://example.com/profile.png"
			));

		// when & then
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.name").value("길동"))
			.andExpect(jsonPath("$.data.nickname").value("길동"))
			.andExpect(jsonPath("$.data.role").value("USER"))
			.andExpect(jsonPath("$.data.provider").value("GOOGLE"))
			.andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.png"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("PATCH /api/users/me — multipart로 닉네임과 프로필 이미지 수정 성공 (200)")
	void updateMyInfoReturnsOkResponse() throws Exception {
		// given
		MockMultipartFile profileImage = new MockMultipartFile(
			"profileImage",
			"file.png",
			MediaType.IMAGE_PNG_VALUE,
			"image".getBytes()
		);
		given(userService.updateMyInfo(eq("새닉네임"), any()))
			.willReturn(new UserInfoResponse(
				1L,
				"user@example.com",
				"새닉네임",
				UserRole.USER,
				AuthProvider.GOOGLE,
				"http://localhost:8080/local-files/profile/1_uuid.png"
			));

		// when & then
		mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
				.file(profileImage)
				.param("nickname", "새닉네임"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.nickname").value("새닉네임"))
			.andExpect(jsonPath("$.data.profileImageUrl").value("http://localhost:8080/local-files/profile/1_uuid.png"))
			.andExpect(jsonPath("$.error").doesNotExist());

		verify(userService).updateMyInfo(eq("새닉네임"), any());
	}

	@Test
	@DisplayName("PATCH /api/users/me/password — 비밀번호 변경 성공 (200)")
	void changeMyPasswordReturnsOkResponse() throws Exception {
		// given
		given(userService.changePassword(any()))
			.willReturn(MemberActionResponse.success());

		// when & then
		mockMvc.perform(patch("/api/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"currentPassword": "password123",
						"newPassword": "newPassword123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.completed").value(true))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("DELETE /api/users/me — 회원 탈퇴 성공 (204)")
	void deleteMyAccountReturnsNoContent() throws Exception {
		// when & then
		mockMvc.perform(delete("/api/users/me"))
			.andExpect(status().isNoContent());

		verify(userService).deleteMyAccount();
	}

	@Test
	@DisplayName("PATCH /api/users/me — 닉네임 중복 시 409 DUPLICATE_NICKNAME")
	void updateMyInfoReturnsConflictWhenNicknameIsDuplicated() throws Exception {
		// given
		given(userService.updateMyInfo(eq("중복닉네임"), any()))
			.willThrow(new BootSignalException(ErrorCode.DUPLICATE_NICKNAME));

		// when & then
		mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
				.param("nickname", "중복닉네임"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("DUPLICATE_NICKNAME"))
			.andExpect(jsonPath("$.error.message").value("이미 사용 중인 닉네임입니다."));
	}

	@Test
	@DisplayName("GET /api/users/me — 미로그인 시 401 UNAUTHORIZED")
	void getMyInfoReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// given
		given(userService.getMyInfo())
			.willThrow(new BootSignalException(ErrorCode.UNAUTHORIZED));

		// when & then
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("GET /api/users/me — 사용자 없음 시 404 USER_NOT_FOUND")
	void getMyInfoReturnsNotFoundWhenUserDoesNotExist() throws Exception {
		// given
		given(userService.getMyInfo())
			.willThrow(new BootSignalException(ErrorCode.USER_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
			.andExpect(jsonPath("$.error.message").value("사용자를 찾을 수 없습니다."));
	}
}
