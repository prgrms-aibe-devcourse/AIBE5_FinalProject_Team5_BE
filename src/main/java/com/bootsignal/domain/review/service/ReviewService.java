package com.bootsignal.domain.review.service;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.review.dto.ReviewCreateRequest;
import com.bootsignal.domain.review.dto.ReviewResponse;
import com.bootsignal.domain.review.dto.ReviewStatisticsResponse;
import com.bootsignal.domain.review.dto.ReviewUpdateRequest;
import com.bootsignal.domain.review.dto.VerifiedReviewDetailRequest;
import com.bootsignal.domain.review.entity.ReviewCompletionStatus;
import com.bootsignal.domain.review.entity.ReviewDropoutMajorReason;
import com.bootsignal.domain.review.entity.ReviewDropoutSubReason;
import com.bootsignal.domain.review.entity.ReviewEmploymentStatus;
import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.entity.ReviewType;
import com.bootsignal.domain.review.entity.ReviewVerifiedDetail;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import com.bootsignal.domain.verification.repository.VerificationRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 리뷰 생성, 수정, 조회와 인증 리뷰 상세 설문 통계 생성을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final VerificationRepository verificationRepository;

    @Transactional
    public ReviewResponse create(Long courseId, ReviewCreateRequest request) {
        User user = getAuthenticatedUser();
        Course course = getCourse(courseId);
        CourseSession courseSession = getCourseSession(request.courseSessionId());
        ReviewVerifiedDetail verifiedDetail = null;

        validateCourseSessionBelongsToCourse(courseSession, courseId);

        if (reviewRepository.existsByUserIdAndCourseSessionId(user.getId(), courseSession.getId())) {
            throw new BootSignalException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        if (request.reviewType() == ReviewType.VERIFIED) {
            verifiedDetail = buildVerifiedDetail(request.verifiedDetail());
            validateApprovedVerification(user.getId(), courseSession.getId());
        } else if (request.verifiedDetail() != null) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "일반 리뷰에는 인증 리뷰 상세 설문을 등록할 수 없습니다.");
        }

        Integer rating = resolveRating(request);
        String content = resolveContent(request);

        Review review = Review.builder()
            .user(user)
            .course(course)
            .courseSession(courseSession)
            .reviewType(request.reviewType())
            .rating(rating)
            .content(content)
            .build();

        if (verifiedDetail != null) {
            review.updateVerifiedDetail(verifiedDetail);
        }

        return ReviewResponse.from(reviewRepository.save(review));
    }

    public Page<ReviewResponse> getList(Long courseId, ReviewType reviewType, Pageable pageable) {
        getCourse(courseId);
        return reviewRepository.findAllByCourseId(courseId, reviewType, pageable)
            .map(ReviewResponse::from);
    }

    public ReviewStatisticsResponse getStatistics(Long courseId) {
        getCourse(courseId);
        return ReviewStatisticsResponse.from(reviewRepository.findAllVerifiedWithDetailByCourseId(courseId));
    }

    public ReviewResponse get(Long reviewId) {
        return ReviewResponse.from(findActiveReview(reviewId));
    }

    @Transactional
    public ReviewResponse update(Long reviewId, ReviewUpdateRequest request) {
        User user = getAuthenticatedUser();
        Review review = findActiveReview(reviewId);

        validateAuthor(review, user);

        review.update(request.rating(), request.content());
        if (request.verifiedDetail() != null) {
            updateVerifiedDetail(review, request.verifiedDetail());
        }

        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse upgrade(Long reviewId, VerifiedReviewDetailRequest request) {
        User user = getAuthenticatedUser();
        Review review = findActiveReview(reviewId);

        validateAuthor(review, user);

        if (review.getReviewType() == ReviewType.VERIFIED) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "이미 인증 리뷰입니다.");
        }

        validateApprovedVerification(user.getId(), review.getCourseSession().getId());

        review.upgradeToVerified();
        review.updateVerifiedDetail(buildVerifiedDetail(request));
        return ReviewResponse.from(review);
    }

    @Transactional
    public void delete(Long reviewId) {
        User user = getAuthenticatedUser();
        Review review = findActiveReview(reviewId);

        validateAuthor(review, user);

        review.softDelete();
    }

    private void validateCourseSessionBelongsToCourse(CourseSession courseSession, Long courseId) {
        if (!courseSession.getCourse().getId().equals(courseId)) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "해당 회차는 요청한 과정에 속하지 않습니다.");
        }
    }

    private void validateApprovedVerification(Long userId, Long courseSessionId) {
        boolean approved = verificationRepository.existsByUserIdAndCourseSessionIdAndStatus(
            userId, courseSessionId, VerificationStatus.APPROVED
        );
        if (!approved) {
            throw new BootSignalException(ErrorCode.VERIFICATION_NOT_APPROVED);
        }
    }

    private ReviewVerifiedDetail buildVerifiedDetail(VerifiedReviewDetailRequest request) {
        validateVerifiedDetail(request);
        return newVerifiedDetail(request);
    }

    private ReviewVerifiedDetail newVerifiedDetail(VerifiedReviewDetailRequest request) {
        return ReviewVerifiedDetail.builder()
            .priorKnowledgeLevel(request.priorKnowledgeLevel())
            .age(request.age())
            .learningGoal(request.learningGoal())
            .attendanceType(request.attendanceType())
            .cohort(request.cohort())
            .courseDifficulty(request.courseDifficulty())
            .progressSpeed(request.progressSpeed())
            .teamProjectDifficulty(request.teamProjectDifficulty())
            .avgSelfStudyHours(request.avgSelfStudyHours())
            .instructorDeliveryRating(request.instructorDeliveryRating())
            .curriculumRating(request.curriculumRating())
            .employmentSupportSatisfactionRating(request.employmentSupportRating())
            .projectCount(request.projectCount())
            .projectAchievementRating(request.projectAchievementRating())
            .toolSupportRating(request.toolSupportRating())
            .mentoringSatisfactionRating(request.mentoringSatisfactionRating())
            .completionStatus(request.completionStatus())
            .dropoutMajorReason(dropoutMajorReason(request))
            .dropoutSubReason(dropoutSubReason(request))
            .employmentStatusIn6Months(employmentStatusIn6Months(request))
            .freeReview(request.collaborationComment())
            .build();
    }

    private Integer resolveRating(ReviewCreateRequest request) {
        if (request.rating() != null) {
            return request.rating();
        }

        if (request.reviewType() == ReviewType.VERIFIED) {
            return Math.round((float) (
                request.verifiedDetail().instructorDeliveryRating()
                    + request.verifiedDetail().curriculumRating()
                    + request.verifiedDetail().employmentSupportRating()
                    + request.verifiedDetail().projectAchievementRating()
                    + request.verifiedDetail().toolSupportRating()
                    + request.verifiedDetail().mentoringSatisfactionRating()
            ) / 6);
        }

        throw new BootSignalException(ErrorCode.BAD_REQUEST, "리뷰 평점은 필수입니다.");
    }

    private String resolveContent(ReviewCreateRequest request) {
        if (StringUtils.hasText(request.content())) {
            return request.content().trim();
        }

        if (request.reviewType() == ReviewType.VERIFIED) {
            String collaborationComment = request.verifiedDetail().collaborationComment();
            return StringUtils.hasText(collaborationComment) ? collaborationComment.trim() : "";
        }

        throw new BootSignalException(ErrorCode.BAD_REQUEST, "리뷰 내용은 필수입니다.");
    }

    private void updateVerifiedDetail(Review review, VerifiedReviewDetailRequest request) {
        if (review.getReviewType() != ReviewType.VERIFIED) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "인증 리뷰만 상세 설문을 수정할 수 있습니다.");
        }

        validateVerifiedDetail(request);
        if (review.getVerifiedDetail() == null) {
            review.updateVerifiedDetail(newVerifiedDetail(request));
            return;
        }

        review.getVerifiedDetail().update(
            request.priorKnowledgeLevel(),
            request.age(),
            request.learningGoal(),
            request.attendanceType(),
            request.cohort(),
            request.courseDifficulty(),
            request.progressSpeed(),
            request.teamProjectDifficulty(),
            request.avgSelfStudyHours(),
            request.instructorDeliveryRating(),
            request.curriculumRating(),
            request.employmentSupportRating(),
            request.projectCount(),
            request.projectAchievementRating(),
            request.toolSupportRating(),
            request.mentoringSatisfactionRating(),
            request.completionStatus(),
            dropoutMajorReason(request),
            dropoutSubReason(request),
            employmentStatusIn6Months(request),
            request.collaborationComment()
        );
    }

    private void validateVerifiedDetail(VerifiedReviewDetailRequest request) {
        if (request == null) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "인증 리뷰 상세 설문은 필수입니다.");
        }

        if (request.completionStatus() == ReviewCompletionStatus.DROPOUT) {
            validateDropoutReason(request.dropoutMajorReason(), request.dropoutSubReason());
        }

        if (request.completionStatus() == ReviewCompletionStatus.COMPLETED
            && request.employmentStatus() == null) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "수료 상태의 인증 리뷰는 취업 상태 입력이 필요합니다.");
        }
    }

    private void validateDropoutReason(
        ReviewDropoutMajorReason majorReason,
        ReviewDropoutSubReason subReason
    ) {
        if (majorReason == null || subReason == null) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "중도 포기 상태의 인증 리뷰는 중도 포기 사유가 필요합니다.");
        }

        if (!subReason.belongsTo(majorReason)) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "중도 포기 대분류와 세부 사유가 일치하지 않습니다.");
        }
    }

    private ReviewDropoutMajorReason dropoutMajorReason(VerifiedReviewDetailRequest request) {
        return request.completionStatus() == ReviewCompletionStatus.DROPOUT ? request.dropoutMajorReason() : null;
    }

    private ReviewDropoutSubReason dropoutSubReason(VerifiedReviewDetailRequest request) {
        return request.completionStatus() == ReviewCompletionStatus.DROPOUT ? request.dropoutSubReason() : null;
    }

    private ReviewEmploymentStatus employmentStatusIn6Months(VerifiedReviewDetailRequest request) {
        return request.completionStatus() == ReviewCompletionStatus.COMPLETED ? request.employmentStatus() : null;
    }

    private void validateAuthor(Review review, User user) {
        if (!review.getUser().getId().equals(user.getId())) {
            throw new BootSignalException(ErrorCode.FORBIDDEN);
        }
    }

    private User getAuthenticatedUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
            .filter(u -> !u.isDeleted())
            .orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND, "해당 과정을 찾을 수 없습니다."));
    }

    private CourseSession getCourseSession(Long courseSessionId) {
        return courseSessionRepository.findById(courseSessionId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.COURSE_SESSION_NOT_FOUND));
    }

    private Review findActiveReview(Long reviewId) {
        return reviewRepository.findActiveByIdWithDetail(reviewId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.REVIEW_NOT_FOUND));
    }
}
