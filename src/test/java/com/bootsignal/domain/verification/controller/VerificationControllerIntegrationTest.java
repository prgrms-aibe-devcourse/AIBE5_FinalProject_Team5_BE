package com.bootsignal.domain.verification.controller;

import static com.bootsignal.support.AuthCookieTestUtils.extractAccessToken;
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
 * 인증 신청 API의 JWT 인증, 관리자 권한, 자료 유형별 업로드/다운로드, 인증 리뷰 연동을 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VerificationControllerIntegrationTest {

    private static final String PASSWORD = "password123";
    private static final String JOB_TRAINING_CONTENT = "job-training-content";
    private static final String ONLINE_APPLICATION_CONTENT = "online-application-content";

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
    void userCanCreateAndReadOwnVerificationWithSeparatedEvidenceFiles() throws Exception {
        CourseSessionFixture fixture = createCourseSessionFixture();
        String userToken = createUserAndLogin("verify-user@example.com", "인증사용자", UserRole.USER);

        Long verificationId = createVerification(userToken, fixture.courseId(), fixture.courseSessionId());

        mockMvc.perform(get("/api/verifications/my")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].verificationId").value(verificationId))
            .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.data.content[0].jobTrainingHistoryFile.fileName")
                .value("job-training-history.txt"))
            .andExpect(jsonPath("$.data.content[0].onlineCourseApplicationFile.fileName")
                .value("online-course-application.txt"));

        mockMvc.perform(get("/api/verifications/{verificationId}/evidence/job-training-history", verificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(JOB_TRAINING_CONTENT));

        mockMvc.perform(get("/api/verifications/{verificationId}/evidence/online-course-application", verificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(ONLINE_APPLICATION_CONTENT));
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
            .andExpect(jsonPath("$.data.content[0].verificationId").value(verificationId))
            .andExpect(jsonPath("$.data.content[0].jobTrainingHistoryFile.fileName")
                .value("job-training-history.txt"));

        mockMvc.perform(get("/api/admin/verifications/{verificationId}/evidence/online-course-application",
                verificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(ONLINE_APPLICATION_CONTENT));

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
                        "content": "승인된 인증으로 작성한 리뷰입니다.",
                        "verifiedDetail": {
                            "priorKnowledgeLevel": "non_major",
                            "age": 29,
                            "learningGoal": "employment",
                            "attendanceType": "online",
                            "cohort": 1,
                            "courseDifficulty": "medium",
                            "progressSpeed": "moderate",
                            "teamProjectDifficulty": "medium",
                            "avgSelfStudyHours": 3,
                            "instructorDeliveryRating": 5,
                            "curriculumRating": 4,
                            "employmentSupportRating": 4,
                            "projectCount": 3,
                            "projectAchievementRating": 5,
                            "toolSupportRating": 4,
                            "mentoringSatisfactionRating": 5,
                            "completionStatus": "completed",
                            "employmentStatus": "preparing",
                            "collaborationComment": "팀 프로젝트 협업 경험이 좋았습니다."
                        }
                    }
                    """.formatted(fixture.courseSessionId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reviewType").value("VERIFIED"))
            .andExpect(jsonPath("$.data.verifiedDetail.priorKnowledgeLevel").value("비전공"))
            .andExpect(jsonPath("$.data.verifiedDetail.employmentSupportSatisfactionRating").value(4));
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
        MockMultipartFile jobTrainingHistoryFile = new MockMultipartFile(
            "jobTrainingHistoryFile",
            "job-training-history.txt",
            "text/plain",
            JOB_TRAINING_CONTENT.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile onlineCourseApplicationFile = new MockMultipartFile(
            "onlineCourseApplicationFile",
            "online-course-application.txt",
            "text/plain",
            ONLINE_APPLICATION_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        String response = mockMvc.perform(multipart("/api/verifications")
                .file(jobTrainingHistoryFile)
                .file(onlineCourseApplicationFile)
                .param("courseId", String.valueOf(courseId))
                .param("courseSessionId", String.valueOf(courseSessionId))
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.jobTrainingHistoryFile.fileName").value("job-training-history.txt"))
            .andExpect(jsonPath("$.data.onlineCourseApplicationFile.fileName")
                .value("online-course-application.txt"))
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
        return extractAccessToken(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn());
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
