package com.bootsignal.domain.inquiry.service;

import com.bootsignal.domain.inquiry.dto.AdminInquiryResponse;
import com.bootsignal.domain.inquiry.dto.InquiryAnswerRequest;
import com.bootsignal.domain.inquiry.entity.Inquiry;
import com.bootsignal.domain.inquiry.entity.InquiryStatus;
import com.bootsignal.domain.inquiry.repository.InquiryRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
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
import static org.mockito.BDDMockito.given;

/**
 * 관리자 문의 서비스가 답변 등록 시 문의 상태와 답변자를 함께 갱신하는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminInquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private UserRepository userRepository;

    private AdminInquiryService adminInquiryService;

    @BeforeEach
    void setUp() {
        adminInquiryService = new AdminInquiryService(inquiryRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void answerCompletesInquiry() {
        User writer = user(1L, "writer@example.com", "작성자");
        User admin = user(2L, "admin@example.com", "관리자");
        Inquiry inquiry = Inquiry.builder()
            .user(writer)
            .title("문의")
            .content("문의 내용")
            .build();
        ReflectionTestUtils.setField(inquiry, "id", 100L);

        setAuthentication(admin.getEmail());
        given(inquiryRepository.findById(100L)).willReturn(Optional.of(inquiry));
        given(userRepository.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));

        AdminInquiryResponse response =
            adminInquiryService.answer(100L, new InquiryAnswerRequest("답변입니다."));

        assertThat(response.status()).isEqualTo(InquiryStatus.COMPLETED);
        assertThat(response.adminReply()).isEqualTo("답변입니다.");
        assertThat(response.answeredById()).isEqualTo(2L);
        assertThat(response.answeredAt()).isNotNull();
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
