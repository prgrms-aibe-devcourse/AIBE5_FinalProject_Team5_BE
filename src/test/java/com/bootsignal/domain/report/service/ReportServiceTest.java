package com.bootsignal.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.post.repository.PostRepository;
import com.bootsignal.domain.report.dto.ReportCreateRequest;
import com.bootsignal.domain.report.dto.ReportResponse;
import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportTargetType;
import com.bootsignal.domain.report.repository.ReportRepository;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 신고 생성 시 인증 사용자와 신고 대상 존재 검증을 확인하는 서비스 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PostRepository postRepository;

	@Mock
	private ReviewRepository reviewRepository;

	private ReportService reportService;

	@BeforeEach
	void setUp() {
		reportService = new ReportService(reportRepository, userRepository, postRepository, reviewRepository);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createSavesReportWhenPostTargetExists() {
		User reporter = user(1L);
		ReportCreateRequest request = new ReportCreateRequest(
			ReportTargetType.POST,
			10L,
			"부적절한 게시글",
			"광고성 게시글입니다."
		);
		setAuthentication(reporter.getEmail());
		given(userRepository.findByEmail(reporter.getEmail())).willReturn(Optional.of(reporter));
		given(postRepository.existsByIdAndDeletedAtIsNullAndIsValidTrue(10L)).willReturn(true);
		given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

		ReportResponse response = reportService.create(request);

		assertThat(response.targetType()).isEqualTo(ReportTargetType.POST);
		assertThat(response.targetId()).isEqualTo(10L);
		verify(reportRepository).save(any(Report.class));
	}

	@Test
	void createThrowsNotFoundWhenTargetDoesNotExist() {
		User reporter = user(1L);
		ReportCreateRequest request = new ReportCreateRequest(
			ReportTargetType.REVIEW,
			20L,
			"부적절한 리뷰",
			"허위 리뷰입니다."
		);
		setAuthentication(reporter.getEmail());
		given(userRepository.findByEmail(reporter.getEmail())).willReturn(Optional.of(reporter));
		given(reviewRepository.existsByIdAndDeletedAtIsNull(20L)).willReturn(false);

		assertThatThrownBy(() -> reportService.create(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.NOT_FOUND);

		verify(reportRepository, never()).save(any());
	}

	@Test
	void createRejectsUnsupportedCommentTarget() {
		User reporter = user(1L);
		ReportCreateRequest request = new ReportCreateRequest(
			ReportTargetType.COMMENT,
			30L,
			"부적절한 댓글",
			"댓글 도메인이 아직 없습니다."
		);
		setAuthentication(reporter.getEmail());
		given(userRepository.findByEmail(reporter.getEmail())).willReturn(Optional.of(reporter));

		assertThatThrownBy(() -> reportService.create(request))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.BAD_REQUEST);

		verify(reportRepository, never()).save(any());
	}

	private void setAuthentication(String email) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(email, "token", List.of())
		);
	}

	private User user(Long id) {
		User user = User.signupLocal("reporter@example.com", "encoded-password", "reporter");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
