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
import com.bootsignal.domain.verification.dto.VerificationEvidenceFile;
import com.bootsignal.domain.verification.dto.VerificationResponse;
import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationEvidenceType;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import com.bootsignal.domain.verification.repository.VerificationRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
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
 * 사용자 인증 신청 서비스의 중복 신청, 과정/회차 검증, 자료별 파일 저장 규칙을 검증합니다.
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
    @DisplayName("직업훈련 이력 자료와 온라인 수강 신청 이력 자료를 구분해 인증 신청을 생성한다")
    void createReturnsPendingVerificationWithSeparatedEvidenceFiles() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
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

        VerificationResponse response = verificationService.create(
            10L,
            20L,
            jobTrainingHistoryFile(),
            onlineCourseApplicationFile()
        );

        assertThat(response.verificationId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(VerificationStatus.PENDING);
        assertThat(response.jobTrainingHistoryFile().fileName()).isEqualTo("job-training-history.txt");
        assertThat(response.jobTrainingHistoryFile().fileSize()).isEqualTo(20L);
        assertThat(response.onlineCourseApplicationFile().fileName()).isEqualTo("online-course-application.txt");
        assertThat(response.onlineCourseApplicationFile().fileSize()).isEqualTo(26L);
    }

    @Test
    @DisplayName("동일 사용자의 동일 과정 회차 중복 신청을 차단한다")
    void createThrowsAlreadyExistsWhenDuplicated() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(verificationRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(true);

        assertThatThrownBy(() -> verificationService.create(
            10L,
            20L,
            jobTrainingHistoryFile(),
            onlineCourseApplicationFile()
        ))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.VERIFICATION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("요청한 과정과 회차가 일치하지 않으면 차단한다")
    void createThrowsBadRequestWhenCourseSessionMismatch() {
        User user = user(1L, "user@example.com");
        Course requestedCourse = course(10L);
        Course anotherCourse = course(11L);
        CourseSession courseSession = courseSession(20L, anotherCourse);
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(requestedCourse));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));

        assertThatThrownBy(() -> verificationService.create(
            10L,
            20L,
            jobTrainingHistoryFile(),
            onlineCourseApplicationFile()
        ))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("직업훈련 이력 자료가 비어 있으면 차단한다")
    void createThrowsEvidenceRequiredWhenJobTrainingHistoryFileIsEmpty() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        MockMultipartFile emptyFile = new MockMultipartFile(
            "jobTrainingHistoryFile",
            "empty.txt",
            "text/plain",
            new byte[0]
        );
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(verificationRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(false);

        assertThatThrownBy(() -> verificationService.create(
            10L,
            20L,
            emptyFile,
            onlineCourseApplicationFile()
        ))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.VERIFICATION_EVIDENCE_REQUIRED);
    }

    @Test
    @DisplayName("온라인 수강 신청 이력 자료가 비어 있으면 차단한다")
    void createThrowsEvidenceRequiredWhenOnlineCourseApplicationFileIsEmpty() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        MockMultipartFile emptyFile = new MockMultipartFile(
            "onlineCourseApplicationFile",
            "empty.txt",
            "text/plain",
            new byte[0]
        );
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(verificationRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(false);

        assertThatThrownBy(() -> verificationService.create(
            10L,
            20L,
            jobTrainingHistoryFile(),
            emptyFile
        ))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.VERIFICATION_EVIDENCE_REQUIRED);
    }

    @Test
    @DisplayName("본인 인증 신청의 자료 유형별 파일을 다운로드 DTO로 반환한다")
    void getMyEvidenceFileReturnsRequestedEvidenceType() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        Verification verification = verification(100L, user, course, courseSession(20L, course));
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(verificationRepository.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(verification));

        VerificationEvidenceFile evidenceFile = verificationService.getMyEvidenceFile(
            100L,
            VerificationEvidenceType.ONLINE_COURSE_APPLICATION
        );

        assertThat(evidenceFile.fileName()).isEqualTo("online-course-application.txt");
        assertThat(evidenceFile.contentType()).isEqualTo("text/plain");
        assertThat(evidenceFile.data()).isEqualTo("online-application-content".getBytes(StandardCharsets.UTF_8));
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

    private Verification verification(Long id, User user, Course course, CourseSession courseSession) {
        Verification verification = Verification.builder()
            .user(user)
            .course(course)
            .courseSession(courseSession)
            .jobTrainingHistoryFileName("job-training-history.txt")
            .jobTrainingHistoryContentType("text/plain")
            .jobTrainingHistoryFileSize(20L)
            .jobTrainingHistoryData("job-training-content".getBytes(StandardCharsets.UTF_8))
            .onlineCourseApplicationFileName("online-course-application.txt")
            .onlineCourseApplicationContentType("text/plain")
            .onlineCourseApplicationFileSize(26L)
            .onlineCourseApplicationData("online-application-content".getBytes(StandardCharsets.UTF_8))
            .build();
        ReflectionTestUtils.setField(verification, "id", id);
        return verification;
    }

    private MockMultipartFile jobTrainingHistoryFile() {
        return new MockMultipartFile(
            "jobTrainingHistoryFile",
            "job-training-history.txt",
            "text/plain",
            "job-training-content".getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile onlineCourseApplicationFile() {
        return new MockMultipartFile(
            "onlineCourseApplicationFile",
            "online-course-application.txt",
            "text/plain",
            "online-application-content".getBytes(StandardCharsets.UTF_8)
        );
    }
}
