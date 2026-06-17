package com.bootsignal.domain.report.service;

import com.bootsignal.domain.report.dto.ReportCreateRequest;
import com.bootsignal.domain.report.dto.ReportResponse;
import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.post.repository.PostRepository;
import com.bootsignal.domain.report.repository.ReportRepository;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 사용자의 신고 생성과 신고 대상 유효성 검증을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ReportResponse create(ReportCreateRequest request) {
        User reporter = getAuthenticatedUser();
        validateReportTarget(request);

        Report report = Report.builder()
            .reporter(reporter)
            .targetType(request.targetType())
            .targetId(request.targetId())
            .reason(request.reason())
            .detail(request.detail())
            .build();

        return ReportResponse.from(reportRepository.save(report));
    }

    private void validateReportTarget(ReportCreateRequest request) {
        boolean exists = switch (request.targetType()) {
            case POST -> postRepository.existsByIdAndDeletedAtIsNullAndIsValidTrue(request.targetId());
            case REVIEW -> reviewRepository.existsByIdAndDeletedAtIsNull(request.targetId());
            case COMMENT -> throw new BootSignalException(ErrorCode.BAD_REQUEST, "댓글 신고 기능은 아직 지원하지 않습니다.");
        };

        if (!exists) {
            throw new BootSignalException(ErrorCode.NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
        }
    }

    private User getAuthenticatedUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
    }
}
