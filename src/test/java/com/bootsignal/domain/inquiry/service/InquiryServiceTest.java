package com.bootsignal.domain.inquiry.service;

import com.bootsignal.domain.inquiry.dto.InquiryCreateRequest;
import com.bootsignal.domain.inquiry.dto.InquiryResponse;
import com.bootsignal.domain.inquiry.entity.Inquiry;
import com.bootsignal.domain.inquiry.repository.InquiryRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 사용자 문의 서비스가 인증 사용자 기준으로 문의를 생성하고 본인 문의만 조회하는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private UserRepository userRepository;

    private InquiryService inquiryService;

    @BeforeEach
    void setUp() {
        inquiryService = new InquiryService(inquiryRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSavesPendingInquiryForCurrentUser() {
        User user = user(1L, "user@example.com", "사용자");
        setAuthentication(user.getEmail());
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(inquiryRepository.save(any(Inquiry.class))).willAnswer(invocation -> {
            Inquiry inquiry = invocation.getArgument(0);
            ReflectionTestUtils.setField(inquiry, "id", 10L);
            return inquiry;
        });

        InquiryResponse response = inquiryService.create(new InquiryCreateRequest(" 문의 제목 ", " 문의 내용 "));

        assertThat(response.inquiryId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("문의 제목");
        assertThat(response.content()).isEqualTo("문의 내용");
        assertThat(response.status().name()).isEqualTo("PENDING");
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    void getMyInquiryRejectsOtherUsersInquiry() {
        User owner = user(1L, "owner@example.com", "작성자");
        User other = user(2L, "other@example.com", "다른사용자");
        Inquiry inquiry = Inquiry.builder()
            .user(other)
            .title("다른 문의")
            .content("다른 문의 내용")
            .build();
        ReflectionTestUtils.setField(inquiry, "id", 20L);

        setAuthentication(owner.getEmail());
        given(userRepository.findByEmail(owner.getEmail())).willReturn(Optional.of(owner));
        given(inquiryRepository.findById(20L)).willReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> inquiryService.getMyInquiry(20L))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private void setAuthentication(String email) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(email, "token", List.of())
        );
    }

    private User user(Long id, String email, String nickname) {
        User user = User.signupLocal(email, "encoded-password", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
