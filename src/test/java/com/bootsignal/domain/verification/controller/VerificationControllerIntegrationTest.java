package com.bootsignal.domain.verification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 신청 API의 실제 JWT 인증, 관리자 권한, 승인 후 인증 리뷰 연동을 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VerificationControllerIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseSessionRepository courseSessionRepository;

    @Test
    void userCanCreateAndReadOwnVerificationWithEvidenceFile() throws Exception {
        CourseSessionFixture fixture = createCourseSessionFixture();
        String userToken = createUserAndLogin("verify-user@example.com", "인증사용자", UserRole.USER);

        Long verificationId = createVerification(userToken, fixture.courseId(), fixture.courseSessionId());

        mockMvc.perform(get("/api/verifications/my")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].verificationId").value(verificationId))
            .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.data.content[0].evidenceFileName").value("evidence.txt"));

        mockMvc.perform(get("/api/verifications/{verificationId}/evidence", verificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string("proof-content"));
    }

    @Test
    void nonAdminUserCannotAccessAdminVerificationApi() throws Exception {
        String userToken = createUserAndLogin("normal-user@example.com", "일반사용자", UserRole.USER);

        mockMvc.perform(get("/api/admin/verifications")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanApproveVerificationAndUserCanCreateVerifiedReview() throws Exception {
        CourseSessionFixture fixture = createCourseSessionFixture();
        String userToken = createUserAndLogin("review-user@example.com", "리뷰사용자", UserRole.USER);
        String adminToken = createUserAndLogin("admin@example.com", "관리자", UserRole.ADMIN);
        Long verificationId = createVerification(userToken, fixture.courseId(), fixture.courseSessionId());

        mockMvc.perform(get("/api/admin/verifications")
                .queryParam("status", "PENDING")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].verificationId").value(verificationId));

        mockMvc.perform(patch("/api/admin/verifications/{verificationId}/approve", verificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "memo": "증빙 확인 완료"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("APPROVED"))
            .andExpect(jsonPath("$.data.adminMemo").value("증빙 확인 완료"));

        mockMvc.perform(post("/api/courses/{courseId}/reviews", fixture.courseId())
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "courseSessionId": %d,
                        "reviewType": "VERIFIED",
                        "rating": 5,
                        "content": "승인된 인증으로 작성한 리뷰입니다."
                    }
                    """.formatted(fixture.courseSessionId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reviewType").value("VERIFIED"));
    }

    @Test
    void adminCanRejectPendingVerificationAndProcessedVerificationCannotBeApprovedAgain() throws Exception {
        CourseSessionFixture fixture = createCourseSessionFixture();
        String userToken = createUserAndLogin("reject-user@example.com", "반려사용자", UserRole.USER);
        String adminToken = createUserAndLogin("reject-admin@example.com", "반려관리자", UserRole.ADMIN);
        Long verificationId = createVerification(userToken, fixture.courseId(), fixture.courseSessionId());

        mockMvc.perform(patch("/api/admin/verifications/{verificationId}/reject", verificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "reason": "증빙 자료가 식별되지 않습니다."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.rejectReason").value("증빙 자료가 식별되지 않습니다."));

        mockMvc.perform(patch("/api/admin/verifications/{verificationId}/approve", verificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VERIFICATION_ALREADY_PROCESSED"));
    }

    private Long createVerification(String userToken, Long courseId, Long courseSessionId) throws Exception {
        MockMultipartFile evidenceFile = new MockMultipartFile(
            "evidenceFile",
            "evidence.txt",
            "text/plain",
            "proof-content".getBytes(StandardCharsets.UTF_8)
        );

        String response = mockMvc.perform(multipart("/api/verifications")
                .file(evidenceFile)
                .param("courseId", String.valueOf(courseId))
                .param("courseSessionId", String.valueOf(courseSessionId))
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.evidenceFileName").value("evidence.txt"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(response).path("data").path("verificationId").asLong();
    }

    private String createUserAndLogin(String email, String nickname, UserRole role) throws Exception {
        User user = User.signupLocal(email, passwordEncoder.encode(PASSWORD), nickname);
        ReflectionTestUtils.setField(user, "role", role);
        userRepository.save(user);
        return login(email);
    }

    private String login(String email) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
    }

    private CourseSessionFixture createCourseSessionFixture() {
        String trprId = "TR" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Course course = courseRepository.save(Course.builder()
            .trprId(trprId)
            .title("백엔드 개발 과정")
            .subTitle("테스트 교육기관")
            .build());
        CourseSession courseSession = courseSessionRepository.save(CourseSession.builder()
            .trprId(trprId)
            .trprDegr(1)
            .traStartDate(LocalDate.of(2026, 7, 1))
            .traEndDate(LocalDate.of(2026, 12, 31))
            .course(course)
            .build());
        return new CourseSessionFixture(course.getId(), courseSession.getId());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record CourseSessionFixture(Long courseId, Long courseSessionId) {
    }
}
