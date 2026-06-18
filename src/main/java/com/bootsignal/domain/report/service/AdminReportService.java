package com.bootsignal.domain.report.service;

import com.bootsignal.domain.comment.entity.Comment;
import com.bootsignal.domain.comment.repository.CommentRepository;
import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.post.repository.PostRepository;
import com.bootsignal.domain.report.dto.AdminReportProcessRequest;
import com.bootsignal.domain.report.dto.AdminReportResponse;
import com.bootsignal.domain.report.dto.ReportTargetSnapshot;
import com.bootsignal.domain.report.entity.ReportAction;
import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import com.bootsignal.domain.report.repository.ReportRepository;
import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 신고 목록/상세 조회와 신고 대상 콘텐츠 처리 조치를 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;

    public Page<AdminReportResponse> getList(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        return reportRepository.findByFilters(status, targetType, pageable)
            .map(this::toResponse);
    }

    public AdminReportResponse get(Long reportId) {
        return toResponse(findReport(reportId));
    }

    @Transactional
    public AdminReportResponse process(Long reportId, AdminReportProcessRequest request) {
        Report report = findReport(reportId);
        ReportStatus status = request.resolvedStatus();
        ReportAction action = request.resolvedAction();

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "이미 처리된 신고입니다.");
        }

        if (status != ReportStatus.COMPLETED) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, "신고 처리는 COMPLETED 상태만 가능합니다.");
        }

        if (action == ReportAction.HIDE) {
            hideTarget(report);
        }

        report.process(status, action, request.reason());
        return toResponse(report);
    }

    private Report findReport(Long reportId) {
        return reportRepository.findById(reportId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.REPORT_NOT_FOUND));
    }

    private AdminReportResponse toResponse(Report report) {
        return AdminReportResponse.from(report, resolveTargetSnapshot(report));
    }

    private ReportTargetSnapshot resolveTargetSnapshot(Report report) {
        return switch (report.getTargetType()) {
            case POST -> postRepository.findById(report.getTargetId())
                .map(this::postSnapshot)
                .orElseGet(() -> ReportTargetSnapshot.missing(report.getTargetId()));
            case COMMENT -> commentRepository.findById(report.getTargetId())
                .map(this::commentSnapshot)
                .orElseGet(() -> ReportTargetSnapshot.missing(report.getTargetId()));
            case REVIEW -> reviewRepository.findById(report.getTargetId())
                .map(this::reviewSnapshot)
                .orElseGet(() -> ReportTargetSnapshot.missing(report.getTargetId()));
        };
    }

    private ReportTargetSnapshot postSnapshot(Post post) {
        return new ReportTargetSnapshot(
            post.getUser().getNickname() + " · " + post.getTitle(),
            post.getContent(),
            "/community/posts/" + post.getId()
        );
    }

    private ReportTargetSnapshot commentSnapshot(Comment comment) {
        return new ReportTargetSnapshot(
            comment.getUser().getNickname() + " · " + comment.getPost().getTitle() + " 댓글",
            comment.getContent(),
            "/community/posts/" + comment.getPost().getId() + "/comments/" + comment.getId()
        );
    }

    private ReportTargetSnapshot reviewSnapshot(Review review) {
        return new ReportTargetSnapshot(
            review.getUser().getNickname() + " · " + review.getCourse().getTitle() + " 리뷰",
            review.getContent(),
            "/courses/" + review.getCourse().getId() + "/reviews/" + review.getId()
        );
    }

    private void hideTarget(Report report) {
        switch (report.getTargetType()) {
            case POST -> postRepository.findById(report.getTargetId()).ifPresent(Post::softDelete);
            case COMMENT -> commentRepository.findById(report.getTargetId()).ifPresent(Comment::softDelete);
            case REVIEW -> reviewRepository.findById(report.getTargetId()).ifPresent(Review::softDelete);
        }
    }
}
