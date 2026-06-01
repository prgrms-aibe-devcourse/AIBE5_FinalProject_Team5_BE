package com.bootsignal.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.auth.dto.SignupRequest;
import com.bootsignal.domain.auth.dto.SignupResponse;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordEncoder);
	}

	@Test
	void signupSavesUserWithEncodedPassword() {
		SignupRequest request = new SignupRequest(" User@Example.COM ", "password123", " tester ");
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByNickname("tester")).willReturn(false);
		given(passwordEncoder.encode("password123")).willReturn("encoded-password");
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = authService.signup(request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User savedUser = captor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
		assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
		assertThat(savedUser.getName()).isEqualTo("tester");
		assertThat(savedUser.getNickname()).isEqualTo("tester");
		assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
		assertThat(savedUser.getProvider()).isEqualTo(AuthProvider.LOCAL);
		assertThat(savedUser.isDeleted()).isFalse();
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.nickname()).isEqualTo("tester");
	}

	@Test
	void signupThrowsDuplicateEmailWhenEmailAlreadyExists() {
		SignupRequest request = new SignupRequest("user@example.com", "password123", "tester");
		given(userRepository.existsByEmail("user@example.com")).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.DUPLICATE_EMAIL);

		verify(passwordEncoder, never()).encode(any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void signupThrowsDuplicateNicknameWhenNicknameAlreadyExists() {
		SignupRequest request = new SignupRequest("user@example.com", "password123", "tester");
		given(userRepository.existsByEmail("user@example.com")).willReturn(false);
		given(userRepository.existsByNickname("tester")).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

		verify(passwordEncoder, never()).encode(any());
		verify(userRepository, never()).save(any());
	}
}
