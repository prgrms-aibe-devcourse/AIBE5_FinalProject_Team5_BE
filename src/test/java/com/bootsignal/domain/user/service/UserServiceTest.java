package com.bootsignal.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.user.dto.UserInfoResponse;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.user.storage.ProfileImageStorage;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

// UserService 단위 테스트 — Repository는 Mock, SecurityContext로 JWT 인증 상태를 시뮬레이션
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private ProfileImageStorage profileImageStorage;

	@Mock
	private MultipartFile multipartFile;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, profileImageStorage);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("로그인된 사용자의 내 정보를 반환한다")
	void getMyInfoReturnsAuthenticatedUser() {
		// given
		User user = googleUser(1L, "user@example.com", "길동");
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		// when
		UserInfoResponse response = userService.getMyInfo();

		// then
		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.nickname()).isEqualTo("길동");
		assertThat(response.role()).isEqualTo(UserRole.USER);
		assertThat(response.provider()).isEqualTo(AuthProvider.GOOGLE);
		assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.png");
	}

	@Test
	@DisplayName("인증 정보가 없으면 UNAUTHORIZED 예외를 던진다")
	void getMyInfoThrowsUnauthorizedWhenNotAuthenticated() {
		// given — SecurityContext에 인증 정보 없음

		// when & then
		assertThatThrownBy(() -> userService.getMyInfo())
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("DB에 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
	void getMyInfoThrowsUserNotFoundWhenUserDoesNotExist() {
		// given
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.getMyInfo())
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("탈퇴(soft delete)된 사용자는 USER_NOT_FOUND 예외를 던진다")
	void getMyInfoThrowsUserNotFoundWhenUserIsDeleted() {
		// given
		User user = googleUser(1L, "user@example.com", "길동");
		ReflectionTestUtils.setField(user, "deleted", true);
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		// when & then
		assertThatThrownBy(() -> userService.getMyInfo())
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("닉네임과 프로필 이미지를 함께 수정한다")
	void updateMyInfoUpdatesNicknameAndProfileImage() {
		// given
		User user = googleUser(1L, "user@example.com", "길동");
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L)).willReturn(false);
		given(multipartFile.isEmpty()).willReturn(false);
		given(profileImageStorage.store(multipartFile, 1L))
			.willReturn("http://localhost:8080/local-files/profile/1_uuid.png");

		// when
		UserInfoResponse response = userService.updateMyInfo("새닉네임", multipartFile);

		// then
		assertThat(response.nickname()).isEqualTo("새닉네임");
		assertThat(response.profileImageUrl()).isEqualTo("http://localhost:8080/local-files/profile/1_uuid.png");
		verify(profileImageStorage).store(eq(multipartFile), eq(1L));
	}

	@Test
	@DisplayName("닉네임만 수정하면 프로필 이미지 저장소를 호출하지 않는다")
	void updateMyInfoUpdatesNicknameOnly() {
		// given
		User user = googleUser(1L, "user@example.com", "길동");
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L)).willReturn(false);

		// when
		UserInfoResponse response = userService.updateMyInfo("새닉네임", null);

		// then
		assertThat(response.nickname()).isEqualTo("새닉네임");
		assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.png");
		verify(profileImageStorage, never()).store(any(), any());
	}

	@Test
	@DisplayName("기존과 동일한 닉네임만내면 변경 없이 즉시 반환한다")
	void updateMyInfoReturnsEarlyWhenNicknameIsUnchangedAndNoImage() {
		// given
		User user = googleUser(1L, "user@example.com", "길동");
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		// when
		UserInfoResponse response = userService.updateMyInfo("길동", null);

		// then
		assertThat(response.nickname()).isEqualTo("길동");
		verify(userRepository, never()).existsByNicknameAndIdNot(any(), any());
		verify(profileImageStorage, never()).store(any(), any());
	}

	@Test
	@DisplayName("기존과 동일한 닉네임과 함께 이미지를내면 닉네임 검사 없이 이미지만 저장한다")
	void updateMyInfoUpdatesImageOnlyWhenNicknameIsUnchanged() {
		// given
		User user = googleUser(1L, "user@example.com", "길동");
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(multipartFile.isEmpty()).willReturn(false);
		given(profileImageStorage.store(multipartFile, 1L))
			.willReturn("http://localhost:8080/local-files/profile/1_uuid.png");

		// when
		UserInfoResponse response = userService.updateMyInfo("길동", multipartFile);

		// then
		assertThat(response.nickname()).isEqualTo("길동");
		assertThat(response.profileImageUrl()).isEqualTo("http://localhost:8080/local-files/profile/1_uuid.png");
		verify(userRepository, never()).existsByNicknameAndIdNot(any(), any());
		verify(profileImageStorage).store(eq(multipartFile), eq(1L));
	}

	@Test
	@DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
	void updateMyInfoThrowsDuplicateNicknameWhenNicknameExists() {
		// given
		User user = googleUser(1L, "user@example.com", "길동");
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L)).willReturn(true);

		// when & then
		assertThatThrownBy(() -> userService.updateMyInfo("새닉네임", null))
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
	}

	@Test
	@DisplayName("프로필 이미지만 수정하면 닉네임은 유지하고 저장소 URL을 반영한다")
	void updateMyInfoUpdatesProfileImageOnly() {
		// given
		User user = googleUser(1L, "user@example.com", "길동");
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(multipartFile.isEmpty()).willReturn(false);
		given(profileImageStorage.store(multipartFile, 1L))
			.willReturn("http://localhost:8080/local-files/profile/1_uuid.png");

		// when
		UserInfoResponse response = userService.updateMyInfo(null, multipartFile);

		// then
		assertThat(response.nickname()).isEqualTo("길동");
		assertThat(response.profileImageUrl()).isEqualTo("http://localhost:8080/local-files/profile/1_uuid.png");
	}

	// SecurityContext에 JWT 인증 상태를 직접 주입한다
	private void setAuthentication(String email, UserRole role) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
			email,
			"token",
			List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
		));
	}

	// 테스트용 Google 가입 사용자를 생성한다
	private User googleUser(Long id, String email, String nickname) {
		User user = User.signupGoogle(
			email,
			"google-subject",
			nickname,
			nickname,
			"https://example.com/profile.png"
		);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
