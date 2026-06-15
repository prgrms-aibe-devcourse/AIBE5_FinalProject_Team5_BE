package com.bootsignal.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.verification.dto.VerificationResponse;
import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import com.bootsignal.domain.verification.repository.VerificationRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.time.LocalDate;
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

/**
 * 관리자 인증 처리 서비스의 승인/반려 상태 전이 규칙을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminVerificationService 테스트")
class AdminVerificationServiceTest {

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private UserRepository userRepository;

    private AdminVerificationService adminVerificationService;

    @BeforeEach
    void setUp() {
        adminVerificationService = new AdminVerificationService(verificationRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("PENDING 인증 신청 승인 성공")
    void approveChangesStatusToApproved() {
        User admin = admin();
        Verification verification = verification(100L);
        setAdminAuthentication();

        given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(verificationRepository.findWithDetailsById(100L)).willReturn(Optional.of(verification));

        VerificationResponse response = adminVerificationService.approve(100L, "확인 완료");

        assertThat(response.status()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(response.adminMemo()).isEqualTo("확인 완료");
        assertThat(response.processedById()).isEqualTo(2L);
        assertThat(response.processedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 처리된 인증 신청은 다시 승인할 수 없음")
    void approveThrowsAlreadyProcessedWhenStatusIsNotPending() {
        User admin = admin();
        Verification verification = verification(100L);
        verification.approve(admin, null);
        setAdminAuthentication();

        given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(verificationRepository.findWithDetailsById(100L)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> adminVerificationService.approve(100L, null))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.VERIFICATION_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("PENDING 인증 신청 반려 성공")
    void rejectChangesStatusToRejected() {
        User admin = admin();
        Verification verification = verification(100L);
        setAdminAuthentication();

        given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
        given(verificationRepository.findWithDetailsById(100L)).willReturn(Optional.of(verification));

        VerificationResponse response = adminVerificationService.reject(100L, "자료 식별 불가");

        assertThat(response.status()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(response.rejectReason()).isEqualTo("자료 식별 불가");
        assertThat(response.processedById()).isEqualTo(2L);
    }

    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            "token",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
    }

    private User admin() {
        User admin = User.signupLocal("admin@example.com", "encoded-password", "admin");
        ReflectionTestUtils.setField(admin, "id", 2L);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        return admin;
    }

    private User user() {
        User user = User.signupLocal("user@example.com", "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Course course() {
        Course course = Course.builder()
            .trprId("TR001")
            .title("백엔드 과정")
            .subTitle("테스트 기관")
            .build();
        ReflectionTestUtils.setField(course, "id", 10L);
        return course;
    }

    private CourseSession courseSession(Course course) {
        CourseSession courseSession = CourseSession.builder()
            .trprId("TR001")
            .trprDegr(1)
            .traStartDate(LocalDate.of(2026, 7, 1))
            .traEndDate(LocalDate.of(2026, 12, 31))
            .course(course)
            .build();
        ReflectionTestUtils.setField(courseSession, "id", 20L);
        return courseSession;
    }

    private Verification verification(Long id) {
        Course course = course();
        Verification verification = Verification.builder()
            .user(user())
            .course(course)
            .courseSession(courseSession(course))
            .evidenceFileName("evidence.txt")
            .evidenceContentType("text/plain")
            .evidenceFileSize(13L)
            .evidenceData("proof-content".getBytes())
            .build();
        ReflectionTestUtils.setField(verification, "id", id);
        return verification;
    }
}
