package com.bootsignal.domain.review.service;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.review.dto.ReviewCreateRequest;
import com.bootsignal.domain.review.dto.ReviewResponse;
import com.bootsignal.domain.review.dto.ReviewUpdateRequest;
import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.entity.ReviewType;
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

        validateCourseSessionBelongsToCourse(courseSession, courseId);

        if (reviewRepository.existsByUserIdAndCourseSessionId(user.getId(), courseSession.getId())) {
            throw new BootSignalException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        if (request.reviewType() == ReviewType.VERIFIED) {
            validateApprovedVerification(user.getId(), courseSession.getId());
        }

        Review review = Review.builder()
            .user(user)
            .course(course)
            .courseSession(courseSession)
            .reviewType(request.reviewType())
            .rating(request.rating())
            .content(request.content())
            .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    public Page<ReviewResponse> getList(Long courseId, ReviewType reviewType, Pageable pageable) {
        getCourse(courseId);
        return reviewRepository.findAllByCourseId(courseId, reviewType, pageable)
            .map(ReviewResponse::from);
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
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse upgrade(Long reviewId) {
        User user = getAuthenticatedUser();
        Review review = findActiveReview(reviewId);

        validateAuthor(review, user);

        if (review.getReviewType() == ReviewType.VERIFIED) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "이미 인증 리뷰입니다.");
        }

        validateApprovedVerification(user.getId(), review.getCourseSession().getId());

        review.upgradeToVerified();
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
        return reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.REVIEW_NOT_FOUND));
    }
}
