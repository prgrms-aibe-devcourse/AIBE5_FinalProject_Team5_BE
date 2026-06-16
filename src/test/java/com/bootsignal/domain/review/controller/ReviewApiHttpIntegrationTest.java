package com.bootsignal.domain.review.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.repository.VerificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 실제 HTTP 서버를 통해 일반 리뷰, 인증 리뷰, 리뷰 목록, 상세, 통계 API 흐름을 검증합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Review API HTTP 통합 테스트")
class ReviewApiHttpIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseSessionRepository courseSessionRepository;

    @Autowired
    private VerificationRepository verificationRepository;

    @Test
    @DisplayName("프론트 리뷰 작성 흐름에 맞춰 일반 리뷰와 인증 리뷰 API가 동작한다")
    void reviewApiWorksWithGeneralAndVerifiedReviewFlow() throws Exception {
        CourseFixture fixture = createCourseFixture();
        String generalToken = signupAndLogin(
            "general-" + UUID.randomUUID() + "@example.com",
            uniqueNickname("일반리뷰어")
        );
        String verifiedEmail = "verified-" + UUID.randomUUID() + "@example.com";
        String verifiedToken = signupAndLogin(verifiedEmail, uniqueNickname("인증리뷰어"));
        approveVerification(verifiedEmail, fixture.course(), fixture.verifiedSession());

        JsonNode generalCreate = post(
            "/api/courses/" + fixture.course().getId() + "/reviews",
            Map.of(
                "courseSessionId", fixture.generalSession().getId(),
                "reviewType", "GENERAL",
                "overallRating", 4,
                "content", "일반 리뷰 작성 흐름으로 등록한 후기입니다."
            ),
            generalToken,
            HttpStatus.CREATED
        );
        Long generalReviewId = generalCreate.path("data").path("reviewId").asLong();
        assertThat(generalCreate.path("success").asBoolean()).isTrue();
        assertThat(generalCreate.path("data").path("reviewType").asText()).isEqualTo("GENERAL");
        assertThat(generalCreate.path("data").path("verifiedDetail").isNull()).isTrue();

        JsonNode verifiedCreate = post(
            "/api/courses/" + fixture.course().getId() + "/reviews",
            verifiedReviewRequest(fixture.verifiedSession().getId()),
            verifiedToken,
            HttpStatus.CREATED
        );
        Long verifiedReviewId = verifiedCreate.path("data").path("reviewId").asLong();
        assertThat(verifiedCreate.path("success").asBoolean()).isTrue();
        assertThat(verifiedCreate.path("data").path("reviewType").asText()).isEqualTo("VERIFIED");
        assertThat(verifiedCreate.path("data").path("rating").asInt()).isEqualTo(5);
        assertThat(verifiedCreate.path("data").path("content").asText())
            .isEqualTo("팀 프로젝트 협업 경험이 좋았습니다.");
        assertThat(verifiedCreate.path("data").path("verifiedDetail").path("priorKnowledgeLevel").asText())
            .isEqualTo("비전공");
        assertThat(verifiedCreate.path("data").path("verifiedDetail").path("employmentStatusIn6Months").asText())
            .isEqualTo("준비중");

        JsonNode generalList = get(
            "/api/courses/" + fixture.course().getId() + "/reviews?reviewType=GENERAL",
            HttpStatus.OK
        );
        assertThat(generalList.path("data").path("content")).hasSize(1);
        assertThat(generalList.path("data").path("content").get(0).path("reviewId").asLong())
            .isEqualTo(generalReviewId);

        JsonNode verifiedDetail = get("/api/reviews/" + verifiedReviewId, HttpStatus.OK);
        assertThat(verifiedDetail.path("data").path("verifiedDetail").path("freeReview").asText())
            .isEqualTo("팀 프로젝트 협업 경험이 좋았습니다.");

        JsonNode statistics = get(
            "/api/courses/" + fixture.course().getId() + "/reviews/statistics",
            HttpStatus.OK
        );
        assertThat(statistics.path("data").path("reviewCount").asLong()).isEqualTo(1);
        assertThat(statistics.path("data").path("averageRating").decimalValue())
            .isEqualByComparingTo("5.0");
        assertThat(statistics.path("data").path("ratingBars").get(0).path("score").asInt()).isEqualTo(5);
        assertThat(statistics.path("data").path("ratingBars").get(0).path("count").asLong()).isEqualTo(1);
        assertThat(statistics.path("data").path("priorKnowledgeDistribution").get(0).path("level").asText())
            .isEqualTo("비전공");
        assertThat(statistics.path("data").path("qualityMetrics").get(0).path("label").asText())
            .isEqualTo("강사 전달력");
    }

    private String signupAndLogin(String email, String nickname) throws Exception {
        post(
            "/api/auth/signup",
            Map.of(
                "email", email,
                "password", PASSWORD,
                "nickname", nickname
            ),
            null,
            HttpStatus.CREATED
        );

        JsonNode login = post(
            "/api/auth/login",
            Map.of(
                "email", email,
                "password", PASSWORD
            ),
            null,
            HttpStatus.OK
        );
        return login.path("data").path("accessToken").asText();
    }

    private void approveVerification(String email, Course course, CourseSession courseSession) {
        User user = userRepository.findByEmail(email).orElseThrow();
        User admin = User.signupLocal(
            "admin-" + UUID.randomUUID() + "@example.com",
            "encoded-password",
            uniqueNickname("관리자")
        );
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);

        Verification verification = Verification.builder()
            .user(user)
            .course(course)
            .courseSession(courseSession)
            .jobTrainingHistoryFileName("job-training-history.txt")
            .jobTrainingHistoryContentType("text/plain")
            .jobTrainingHistoryFileSize(20L)
            .jobTrainingHistoryData("job-training-content".getBytes())
            .onlineCourseApplicationFileName("online-course-application.txt")
            .onlineCourseApplicationContentType("text/plain")
            .onlineCourseApplicationFileSize(26L)
            .onlineCourseApplicationData("online-application-content".getBytes())
            .build();
        verification.approve(admin, "증빙 확인 완료");
        verificationRepository.save(verification);
    }

    private CourseFixture createCourseFixture() {
        String trprId = "TR" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Course course = courseRepository.save(Course.builder()
            .trprId(trprId)
            .title("백엔드 개발 과정")
            .subTitle("테스트 교육기관")
            .build());
        CourseSession generalSession = courseSessionRepository.save(CourseSession.builder()
            .trprId(trprId)
            .trprDegr(1)
            .traStartDate(LocalDate.of(2026, 7, 1))
            .traEndDate(LocalDate.of(2026, 12, 31))
            .course(course)
            .build());
        CourseSession verifiedSession = courseSessionRepository.save(CourseSession.builder()
            .trprId(trprId)
            .trprDegr(2)
            .traStartDate(LocalDate.of(2027, 1, 1))
            .traEndDate(LocalDate.of(2027, 6, 30))
            .course(course)
            .build());
        return new CourseFixture(course, generalSession, verifiedSession);
    }

    private Map<String, Object> verifiedReviewRequest(Long courseSessionId) {
        return Map.of(
            "courseSessionId", courseSessionId,
            "reviewType", "VERIFIED",
            "verifiedDetail", Map.ofEntries(
                Map.entry("priorKnowledgeLevel", "non_major"),
                Map.entry("age", "29"),
                Map.entry("learningGoal", "employment"),
                Map.entry("attendanceType", "online"),
                Map.entry("cohort", "1"),
                Map.entry("courseDifficulty", "medium"),
                Map.entry("progressSpeed", "moderate"),
                Map.entry("teamProjectDifficulty", "medium"),
                Map.entry("avgSelfStudyHours", "3"),
                Map.entry("instructorDeliveryRating", 5),
                Map.entry("curriculumRating", 4),
                Map.entry("employmentSupportRating", 4),
                Map.entry("projectCount", "3"),
                Map.entry("projectAchievementRating", 5),
                Map.entry("toolSupportRating", 4),
                Map.entry("mentoringSatisfactionRating", 5),
                Map.entry("completionStatus", "completed"),
                Map.entry("employmentStatus", "preparing"),
                Map.entry("collaborationComment", "팀 프로젝트 협업 경험이 좋았습니다.")
            )
        );
    }

    private String uniqueNickname(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private JsonNode get(String uri, HttpStatus expectedStatus) throws Exception {
        return exchange(HttpMethod.GET, uri, null, null, expectedStatus);
    }

    private JsonNode post(String uri, Object body, String token, HttpStatus expectedStatus) throws Exception {
        return exchange(HttpMethod.POST, uri, body, token, expectedStatus);
    }

    private JsonNode exchange(
        HttpMethod method,
        String uri,
        Object body,
        String token,
        HttpStatus expectedStatus
    ) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Optional.ofNullable(token).ifPresent(value -> headers.setBearerAuth(value));

        ResponseEntity<String> response = restTemplate.exchange(
            uri,
            method,
            new HttpEntity<>(body, headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.getBody());
    }

    private record CourseFixture(
        Course course,
        CourseSession generalSession,
        CourseSession verifiedSession
    ) {
    }
}
