package com.bootsignal.domain.bookmark.dto;

import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.institution.entity.Institution;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookmarkListResponse(
	Long bookmarkId,
	Long courseSessionId,
	LocalDateTime createdAt,
	LocalDate startDate,
	LocalDate endDate,
	BookmarkCourseSessionSummary courseSession,
	BookmarkCourseSummary course,
	BookmarkInstitutionSummary institution
) {
	/* 북마크 목록 응답 생성 */
	public static BookmarkListResponse from(Bookmark bookmark) {
		CourseSession courseSession = bookmark.getCourseSession();
		Course course = courseSession.getCourse();
		Institution institution = course.getInstitution();

		return new BookmarkListResponse(
			bookmark.getId(),
			courseSession.getId(),
			bookmark.getCreatedAt(),
			bookmark.getStartDate(),
			bookmark.getEndDate(),
			BookmarkCourseSessionSummary.from(courseSession),
			BookmarkCourseSummary.from(course),
			institution != null ? BookmarkInstitutionSummary.from(institution) : null
		);
	}

	// coures session 요약 정보
	public record BookmarkCourseSessionSummary(
		Integer trprDegr,
		Integer recruitmentCount,
		Integer selectedTraineeCount,
		Integer confirmedTraineeCount,
		Integer selfPaymentAmount
	) {
		public static BookmarkCourseSessionSummary from(CourseSession courseSession) {
			return new BookmarkCourseSessionSummary(
				courseSession.getTrprDegr(),
				courseSession.getRecruitmentCount(),
				courseSession.getSelectedTraineeCount(),
				courseSession.getConfirmedTraineeCount(),
				courseSession.getSelfPaymentAmount()
			);
		}
	}

	// coures 요약 정보
	public record BookmarkCourseSummary(
		Long id,
		String title,
		BigDecimal stdgScor
	) {
		public static BookmarkCourseSummary from(Course course) {
			return new BookmarkCourseSummary(
				course.getId(),
				course.getTitle(),
				course.getStdgScor()
			);
		}
	}

	// institution 요약 정보
	public record BookmarkInstitutionSummary(
		Long id,
		String institutionName,
		String profileImageUrl,
		String address
	) {
		public static BookmarkInstitutionSummary from(Institution institution) {
			return new BookmarkInstitutionSummary(
				institution.getId(),
				institution.getInstitutionName(),
				institution.getProfileImageUrl(),
				institution.getAddress()
			);
		}
	}
}
