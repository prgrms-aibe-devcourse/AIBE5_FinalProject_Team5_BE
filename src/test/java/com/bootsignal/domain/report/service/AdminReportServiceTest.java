package com.bootsignal.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bootsignal.domain.comment.entity.Comment;
import com.bootsignal.domain.comment.repository.CommentRepository;
import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.post.entity.PostType;
import com.bootsignal.domain.post.repository.PostRepository;
import com.bootsignal.domain.report.dto.AdminReportProcessRequest;
import com.bootsignal.domain.report.dto.AdminReportResponse;
import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportAction;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import com.bootsignal.domain.report.repository.ReportRepository;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 관리자 신고 처리 시 상태 변경과 신고 대상 콘텐츠 조치를 확인하는 서비스 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReviewRepository reviewRepository;

    private AdminReportService adminReportService;

    @BeforeEach
    void setUp() {
        adminReportService = new AdminReportService(
            reportRepository,
            postRepository,
            commentRepository,
            reviewRepository
        );
    }

    @Test
    void processHidesCommentTargetAndReturnsCompletedResponse() {
        User reporter = user(1L, "reporter");
        User writer = user(2L, "writer");
        Post post = post(writer, 10L);
        Comment comment = comment(post, writer, 30L);
        Report report = report(reporter, 100L, ReportTargetType.COMMENT, 30L);
        AdminReportProcessRequest request = new AdminReportProcessRequest(
            null,
            null,
            ReportAction.HIDE,
            null
        );
        given(reportRepository.findById(100L)).willReturn(Optional.of(report));
        given(commentRepository.findById(30L)).willReturn(Optional.of(comment));

        AdminReportResponse response = adminReportService.process(100L, request);

        assertThat(response.status()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(response.contentAction()).isEqualTo(ReportAction.HIDE);
        assertThat(response.type()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(response.contentUrl()).isEqualTo("/community/posts/10/comments/30");
        assertThat(comment.isValid()).isFalse();
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    void processThrowsBadRequestWhenReportAlreadyCompleted() {
        User reporter = user(1L, "reporter");
        Report report = report(reporter, 100L, ReportTargetType.COMMENT, 30L);
        report.process(ReportStatus.COMPLETED, ReportAction.HIDE, "이미 처리되었습니다.");
        AdminReportProcessRequest request = new AdminReportProcessRequest(
            null,
            null,
            ReportAction.INVALID_REASON,
            "재처리 시도"
        );
        given(reportRepository.findById(100L)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReportService.process(100L, request))
            .isInstanceOf(BootSignalException.class)
            .extracting(exception -> ((BootSignalException) exception).errorCode())
            .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    private Report report(User reporter, Long id, ReportTargetType targetType, Long targetId) {
        Report report = Report.builder()
            .reporter(reporter)
            .targetType(targetType)
            .targetId(targetId)
            .reason("스팸")
            .detail("반복 광고 댓글입니다.")
            .build();
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    private Comment comment(Post post, User user, Long id) {
        Comment comment = Comment.builder()
            .post(post)
            .user(user)
            .content("광고 댓글")
            .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private Post post(User user, Long id) {
        Post post = Post.builder()
            .user(user)
            .postType(PostType.QNA)
            .title("Q&A 게시글")
            .content("질문 본문")
            .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private User user(Long id, String nickname) {
        User user = User.signupLocal(nickname + "@example.com", "encoded-password", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
