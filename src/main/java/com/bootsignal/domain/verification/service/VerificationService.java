package com.bootsignal.domain.verification.service;

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
import com.bootsignal.domain.verification.storage.VerificationFileStorage;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로그인 사용자의 인증 신청 생성, 본인 신청 조회, 자료 다운로드를 처리하는 서비스입니다.
 */
@Service
@Transactional(readOnly = true)
public class VerificationService {

    private static final String DEFAULT_EVIDENCE_CONTENT_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    private static final String VERIFICATION_S3_KEY_PREFIX = "verifications";

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final VerificationFileStorage verificationFileStorage;

    public VerificationService(
        VerificationRepository verificationRepository,
        UserRepository userRepository,
        CourseRepository courseRepository,
        CourseSessionRepository courseSessionRepository,
        VerificationFileStorage verificationFileStorage
    ) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.courseSessionRepository = courseSessionRepository;
        this.verificationFileStorage = verificationFileStorage;
    }

    @Transactional
    public VerificationResponse create(
        Long courseId,
        Long courseSessionId,
        MultipartFile jobTrainingHistoryFile,
        MultipartFile onlineCourseApplicationFile
    ) {
        User user = getAuthenticatedUser();
        Course course = getCourse(courseId);
        CourseSession courseSession = getCourseSession(courseSessionId);

        validateCourseSessionBelongsToCourse(courseSession, course);
        validateNotDuplicated(user.getId(), courseSession.getId());

        validateEvidenceFile(jobTrainingHistoryFile, VerificationEvidenceType.JOB_TRAINING_HISTORY);
        validateEvidenceFile(onlineCourseApplicationFile, VerificationEvidenceType.ONLINE_COURSE_APPLICATION);

        String jobTrainingHistoryS3Key = verificationFileStorage.upload(
            jobTrainingHistoryFile,
            VERIFICATION_S3_KEY_PREFIX + "/" + VerificationEvidenceType.JOB_TRAINING_HISTORY.pathSegment()
        );
        String onlineCourseApplicationS3Key = verificationFileStorage.upload(
            onlineCourseApplicationFile,
            VERIFICATION_S3_KEY_PREFIX + "/" + VerificationEvidenceType.ONLINE_COURSE_APPLICATION.pathSegment()
        );

        Verification verification = Verification.builder()
            .user(user)
            .course(course)
            .courseSession(courseSession)
            .jobTrainingHistoryFileName(resolveFileName(jobTrainingHistoryFile, VerificationEvidenceType.JOB_TRAINING_HISTORY))
            .jobTrainingHistoryContentType(resolveContentType(jobTrainingHistoryFile))
            .jobTrainingHistoryFileSize(jobTrainingHistoryFile.getSize())
            .jobTrainingHistoryS3Key(jobTrainingHistoryS3Key)
            .onlineCourseApplicationFileName(resolveFileName(onlineCourseApplicationFile, VerificationEvidenceType.ONLINE_COURSE_APPLICATION))
            .onlineCourseApplicationContentType(resolveContentType(onlineCourseApplicationFile))
            .onlineCourseApplicationFileSize(onlineCourseApplicationFile.getSize())
            .onlineCourseApplicationS3Key(onlineCourseApplicationS3Key)
            .build();

        return VerificationResponse.from(verificationRepository.save(verification));
    }

    public Page<VerificationResponse> getMyList(VerificationStatus status, Pageable pageable) {
        User user = getAuthenticatedUser();
        return verificationRepository.findMyVerifications(user.getId(), status, pageable)
            .map(VerificationResponse::from);
    }

    public VerificationResponse getMyVerification(Long verificationId) {
        User user = getAuthenticatedUser();
        return VerificationResponse.from(findMyVerification(verificationId, user.getId()));
    }

    public VerificationEvidenceFile getMyEvidenceFile(
        Long verificationId,
        VerificationEvidenceType evidenceType
    ) {
        User user = getAuthenticatedUser();
        Verification verification = findMyVerification(verificationId, user.getId());
        return VerificationEvidenceFileResolver.toEvidenceFile(verification, evidenceType, verificationFileStorage);
    }

    private Verification findMyVerification(Long verificationId, Long userId) {
        return verificationRepository.findByIdAndUserId(verificationId, userId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    private void validateCourseSessionBelongsToCourse(CourseSession courseSession, Course course) {
        if (courseSession.getCourse() == null || !courseSession.getCourse().getId().equals(course.getId())) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "해당 회차는 요청한 과정에 속하지 않습니다.");
        }
    }

    private void validateNotDuplicated(Long userId, Long courseSessionId) {
        if (verificationRepository.existsByUserIdAndCourseSessionId(userId, courseSessionId)) {
            throw new BootSignalException(ErrorCode.VERIFICATION_ALREADY_EXISTS);
        }
    }

    private void validateEvidenceFile(MultipartFile evidenceFile, VerificationEvidenceType evidenceType) {
        if (evidenceFile == null || evidenceFile.isEmpty()) {
            throw new BootSignalException(
                ErrorCode.VERIFICATION_EVIDENCE_REQUIRED,
                evidenceType.displayName() + "는 필수입니다."
            );
        }
    }

    private String resolveFileName(MultipartFile evidenceFile, VerificationEvidenceType evidenceType) {
        String originalFileName = evidenceFile.getOriginalFilename();
        if (!StringUtils.hasText(originalFileName)) {
            return evidenceType.defaultFileName();
        }
        return StringUtils.cleanPath(originalFileName);
    }

    private String resolveContentType(MultipartFile evidenceFile) {
        String contentType = evidenceFile.getContentType();
        return StringUtils.hasText(contentType) ? contentType : DEFAULT_EVIDENCE_CONTENT_TYPE;
    }

    private User getAuthenticatedUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.COURSE_NOT_FOUND));
    }

    private CourseSession getCourseSession(Long courseSessionId) {
        return courseSessionRepository.findById(courseSessionId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.COURSE_SESSION_NOT_FOUND));
    }
}
