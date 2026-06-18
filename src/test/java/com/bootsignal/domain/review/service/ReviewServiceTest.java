package com.bootsignal.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.review.dto.ReviewCreateRequest;
import com.bootsignal.domain.review.dto.ReviewResponse;
import com.bootsignal.domain.review.dto.ReviewUpdateRequest;
import com.bootsignal.domain.review.dto.VerifiedReviewDetailRequest;
import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.entity.ReviewAttendanceType;
import com.bootsignal.domain.review.entity.ReviewCompletionStatus;
import com.bootsignal.domain.review.entity.ReviewDifficultyLevel;
import com.bootsignal.domain.review.entity.ReviewEmploymentStatus;
import com.bootsignal.domain.review.entity.ReviewLearningGoal;
import com.bootsignal.domain.review.entity.ReviewPriorKnowledgeLevel;
import com.bootsignal.domain.review.entity.ReviewProgressSpeed;
import com.bootsignal.domain.review.entity.ReviewType;
import com.bootsignal.domain.review.entity.ReviewVerifiedDetail;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 리뷰 서비스의 일반 리뷰, 인증 리뷰, 인증 리뷰 상세 설문 수정 규칙을 repository mock으로 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 테스트")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSessionRepository courseSessionRepository;

    @Mock
    private VerificationRepository verificationRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
            reviewRepository,
            userRepository,
            courseRepository,
            courseSessionRepository,
            verificationRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("일반 리뷰 작성 시 평점과 내용을 저장한다")
    void createGeneralReviewReturnsReviewResponse() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(reviewRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(false);
        given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 100L);
            return review;
        });

        ReviewResponse response = reviewService.create(
            10L,
            new ReviewCreateRequest(20L, ReviewType.GENERAL, 4, "  좋은 과정입니다.  ", null)
        );

        assertThat(response.reviewId()).isEqualTo(100L);
        assertThat(response.reviewType()).isEqualTo(ReviewType.GENERAL);
        assertThat(response.userProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(response.courseTitle()).isEqualTo("백엔드 개발 과정");
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.content()).isEqualTo("좋은 과정입니다.");
        assertThat(response.verifiedDetail()).isNull();
    }

    @Test
    @DisplayName("인증 리뷰 작성 시 상세 설문 점수로 평점을 계산하고 협업 후기를 본문으로 사용한다")
    void createVerifiedReviewCalculatesRatingFromSurvey() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(reviewRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(false);
        given(verificationRepository.existsByUserIdAndCourseSessionIdAndStatus(
            1L,
            20L,
            VerificationStatus.APPROVED
        )).willReturn(true);
        given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 101L);
            return review;
        });

        ReviewResponse response = reviewService.create(
            10L,
            new ReviewCreateRequest(20L, ReviewType.VERIFIED, null, null, verifiedDetailRequest(4))
        );

        assertThat(response.reviewType()).isEqualTo(ReviewType.VERIFIED);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.content()).isEqualTo("협업 경험이 좋았습니다.");
        assertThat(response.verifiedDetail().instructorDeliveryRating()).isEqualTo(4);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 인증 리뷰를 작성할 수 없다")
    void createVerifiedReviewThrowsWhenVerificationIsNotApproved() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseSessionRepository.findById(20L)).willReturn(Optional.of(courseSession));
        given(reviewRepository.existsByUserIdAndCourseSessionId(1L, 20L)).willReturn(false);
        given(verificationRepository.existsByUserIdAndCourseSessionIdAndStatus(
            1L,
            20L,
            VerificationStatus.APPROVED
        )).willReturn(false);

        assertThatThrownBy(() -> reviewService.create(
            10L,
            new ReviewCreateRequest(20L, ReviewType.VERIFIED, null, null, verifiedDetailRequest(4))
        ))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.VERIFICATION_NOT_APPROVED);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("인증 리뷰 상세 설문만 수정해도 상세 점수 평균으로 평점을 갱신한다")
    void updateVerifiedReviewRecalculatesRatingWhenSurveyChanges() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        Review review = review(100L, user, course, courseSession, ReviewType.VERIFIED, 5, "기존 인증 리뷰");
        review.updateVerifiedDetail(verifiedDetail(5));
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(reviewRepository.findActiveByIdWithDetail(100L)).willReturn(Optional.of(review));

        ReviewResponse response = reviewService.update(
            100L,
            new ReviewUpdateRequest(null, "수정된 인증 리뷰", verifiedDetailRequest(2))
        );

        assertThat(response.rating()).isEqualTo(2);
        assertThat(response.content()).isEqualTo("수정된 인증 리뷰");
        assertThat(response.verifiedDetail().instructorDeliveryRating()).isEqualTo(2);
        assertThat(response.verifiedDetail().mentoringSatisfactionRating()).isEqualTo(2);
    }

    @Test
    @DisplayName("일반 리뷰에 인증 리뷰 상세 설문을 수정 요청하면 차단한다")
    void updateGeneralReviewThrowsWhenVerifiedDetailIsSubmitted() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        Review review = review(100L, user, course, courseSession, ReviewType.GENERAL, 4, "일반 리뷰");
        setAuthentication("user@example.com");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(reviewRepository.findActiveByIdWithDetail(100L)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(
            100L,
            new ReviewUpdateRequest(null, "수정된 일반 리뷰", verifiedDetailRequest(2))
        ))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("과정별 리뷰 조회 시 일반 리뷰와 인증 리뷰 모두 작성자 프로필 이미지를 포함한다")
    void getListReturnsProfileImageForGeneralAndVerifiedReviews() {
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        Review generalReview = review(
            100L,
            user(1L, "general@example.com"),
            course,
            courseSession,
            ReviewType.GENERAL,
            5,
            "일반 리뷰"
        );
        Review verifiedReview = review(
            101L,
            user(2L, "verified@example.com"),
            course,
            courseSession,
            ReviewType.VERIFIED,
            4,
            "인증 리뷰"
        );
        Pageable pageable = PageRequest.of(0, 10);
        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(reviewRepository.findAllByCourseId(10L, null, pageable))
            .willReturn(new PageImpl<>(List.of(generalReview, verifiedReview), pageable, 2));

        Page<ReviewResponse> response = reviewService.getList(10L, null, pageable);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent())
            .extracting(ReviewResponse::userProfileImageUrl)
            .containsExactly("https://example.com/profile.png", "https://example.com/profile.png");
        assertThat(response.getContent())
            .extracting(ReviewResponse::reviewType)
            .containsExactly(ReviewType.GENERAL, ReviewType.VERIFIED);
    }

    @Test
    @DisplayName("최신 리뷰 조회 시 전체 과정의 활성 리뷰를 요청한 개수만큼 최신순으로 조회한다")
    void getLatestReviewsReturnsLatestActiveReviews() {
        User user = user(1L, "user@example.com");
        Course course = course(10L);
        CourseSession courseSession = courseSession(20L, course);
        Review review = review(100L, user, course, courseSession, ReviewType.GENERAL, 5, "최신 리뷰");
        given(reviewRepository.findLatestActiveReviews(any(Pageable.class))).willReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getLatestReviews(5);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).reviewId()).isEqualTo(100L);
        assertThat(responses.get(0).userProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(responses.get(0).courseTitle()).isEqualTo("백엔드 개발 과정");
        assertThat(responses.get(0).content()).isEqualTo("최신 리뷰");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findLatestActiveReviews(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("최신 리뷰 조회 개수가 허용 범위를 벗어나면 차단한다")
    void getLatestReviewsThrowsWhenLimitIsOutOfRange() {
        assertThatThrownBy(() -> reviewService.getLatestReviews(21))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.BAD_REQUEST);

        verify(reviewRepository, never()).findLatestActiveReviews(any(Pageable.class));
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
        ReflectionTestUtils.setField(user, "role", UserRole.USER);
        ReflectionTestUtils.setField(user, "profileImageUrl", "https://example.com/profile.png");
        return user;
    }

    private Course course(Long id) {
        Course course = Course.builder()
            .trprId("TR" + id)
            .title("백엔드 개발 과정")
            .subTitle("테스트 교육기관")
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

    private Review review(
        Long id,
        User user,
        Course course,
        CourseSession courseSession,
        ReviewType reviewType,
        Integer rating,
        String content
    ) {
        Review review = Review.builder()
            .user(user)
            .course(course)
            .courseSession(courseSession)
            .reviewType(reviewType)
            .rating(rating)
            .content(content)
            .build();
        ReflectionTestUtils.setField(review, "id", id);
        return review;
    }

    private VerifiedReviewDetailRequest verifiedDetailRequest(Integer score) {
        return new VerifiedReviewDetailRequest(
            ReviewPriorKnowledgeLevel.NON_MAJOR,
            29,
            ReviewLearningGoal.EMPLOYMENT,
            ReviewAttendanceType.ONLINE,
            1,
            ReviewDifficultyLevel.MEDIUM,
            ReviewProgressSpeed.MODERATE,
            ReviewDifficultyLevel.MEDIUM,
            3,
            score,
            score,
            score,
            3,
            score,
            score,
            score,
            ReviewCompletionStatus.COMPLETED,
            null,
            null,
            ReviewEmploymentStatus.PREPARING,
            "협업 경험이 좋았습니다."
        );
    }

    private ReviewVerifiedDetail verifiedDetail(Integer score) {
        return ReviewVerifiedDetail.builder()
            .priorKnowledgeLevel(ReviewPriorKnowledgeLevel.NON_MAJOR)
            .age(29)
            .learningGoal(ReviewLearningGoal.EMPLOYMENT)
            .attendanceType(ReviewAttendanceType.ONLINE)
            .cohort(1)
            .courseDifficulty(ReviewDifficultyLevel.MEDIUM)
            .progressSpeed(ReviewProgressSpeed.MODERATE)
            .teamProjectDifficulty(ReviewDifficultyLevel.MEDIUM)
            .avgSelfStudyHours(3)
            .instructorDeliveryRating(score)
            .curriculumRating(score)
            .employmentSupportSatisfactionRating(score)
            .projectCount(3)
            .projectAchievementRating(score)
            .toolSupportRating(score)
            .mentoringSatisfactionRating(score)
            .completionStatus(ReviewCompletionStatus.COMPLETED)
            .employmentStatusIn6Months(ReviewEmploymentStatus.PREPARING)
            .freeReview("협업 경험이 좋았습니다.")
            .build();
    }
}
