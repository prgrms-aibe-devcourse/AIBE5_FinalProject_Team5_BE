package com.bootsignal.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.verification.dto.VerificationResponse;
import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import com.bootsignal.domain.verification.repository.VerificationRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 사용자 인증 신청 서비스의 중복 신청, 과정/회차 검증, 증빙 파일 저장 규칙을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationService 테스트")
class VerificationServiceTest {

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSessionRepository courseSessionRepository;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(
            verificationRepository,
            userRepository,
            courseRepository,
            courseSessionRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 신청 생성 성공")
    void createReturnsPendingVerification() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        MockMultipartFile evidenceFile = evidenceFile();
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(verificationRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(false);
        given(verificationRepository.save(any(Verification.class))).willAnswer(invocation -> {
            Verification verification = invocation.getArgument(0);
            ReflectionTestUtils.setField(verification, "id", 100L);
            ReflectionTestUtils.setField(verification, "createdAt", LocalDateTime.of(2026, 6, 15, 12, 0));
            return verification;
        });

        VerificationResponse response = verificationService.create(10L, 20L, evidenceFile);

        assertThat(response.verificationId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(VerificationStatus.PENDING);
        assertThat(response.evidenceFileName()).isEqualTo("evidence.txt");
        assertThat(response.evidenceFileSize()).isEqualTo(13L);
    }

    @Test
    @DisplayName("동일 사용자와 동일 과정 회차의 중복 신청은 차단")
    void createThrowsAlreadyExistsWhenDuplicated() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(verificationRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(true);

        assertThatThrownBy(() -> verificationService.create(10L, 20L, evidenceFile()))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.VERIFICATION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("요청한 과정과 회차가 일치하지 않으면 차단")
    void createThrowsBadRequestWhenCourseSessionMismatch() {
        User user = user(1L, "user@example.com");
        Course requestedCourse = course(10L);
        Course anotherCourse = course(11L);
        CourseSession courseSession = courseSession(20L, anotherCourse);
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(requestedCourse));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));

        assertThatThrownBy(() -> verificationService.create(10L, 20L, evidenceFile()))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("증빙 파일이 비어 있으면 차단")
    void createThrowsEvidenceRequiredWhenFileIsEmpty() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        MockMultipartFile emptyFile = new MockMultipartFile("evidenceFile", "empty.txt", "text/plain", new byte[0]);
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(verificationRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(false);

        assertThatThrownBy(() -> verificationService.create(10L, 20L, emptyFile))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.VERIFICATION_EVIDENCE_REQUIRED);
    }

    private void setAuthentication(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            email,
            "token",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    private User user(Long id, String email) {
        User user = User.signupLocal(email, "encoded-password", "tester");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Course course(Long id) {
        Course course = Course.builder()
            .trprId("TR" + id)
            .title("백엔드 과정 " + id)
            .subTitle("테스트 기관")
            .build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private CourseSession courseSession(Long id, Course course) {
        CourseSession courseSession = CourseSession.builder()
            .trprId(course.getTrprId())
            .trprDegr(1)
            .traStartDate(LocalDate.of(2026, 7, 1))
            .traEndDate(LocalDate.of(2026, 12, 31))
            .course(course)
            .build();
        ReflectionTestUtils.setField(courseSession, "id", id);
        return courseSession;
    }

    private MockMultipartFile evidenceFile() {
        return new MockMultipartFile(
            "evidenceFile",
            "evidence.txt",
            "text/plain",
            "proof-content".getBytes()
        );
    }
}
