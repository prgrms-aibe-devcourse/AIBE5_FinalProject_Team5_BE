package com.bootsignal.domain.bookmark.service;

import com.bootsignal.domain.bookmark.dto.BookmarkCreateResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkDeleteResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkListResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkPageResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkSort;
import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.bookmark.repository.BookmarkRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

	private final BookmarkRepository bookmarkRepository;
	private final UserRepository userRepository;
	private final CourseSessionRepository courseSessionRepository;

	/* 북마크 생성 */
	@Transactional
	public BookmarkCreateResponse create(Long courseSessionId) {
		User user = getAuthenticatedUser();
		CourseSession courseSession = getCourseSession(courseSessionId);
		
		// 북마크 중복 검사
		if (bookmarkRepository.existsByUserIdAndCourseSessionId(user.getId(), courseSession.getId())) {
			throw new BootSignalException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
		}

		// 과정 시작&종료 일자 조회
		LocalDate startDate = courseSession.getTraStartDate();
		LocalDate endDate = courseSession.getTraEndDate();
		if (startDate == null || endDate == null) {
			throw new BootSignalException(ErrorCode.COURSE_SESSION_NOT_FOUND);
		}

		// 북마크 생성
		Bookmark bookmark = Bookmark.builder()
			.user(user)
			.courseSession(courseSession)
			.startDate(startDate)
			.endDate(endDate)
			.build();

		return BookmarkCreateResponse.from(bookmarkRepository.save(bookmark));
	}

	/* 북마크 삭제 */
	@Transactional
	public BookmarkDeleteResponse delete(Long courseSessionId) {
		User user = getAuthenticatedUser();

		// 북마크 조회
		Bookmark bookmark = bookmarkRepository
			.findByUserIdAndCourseSessionId(user.getId(), courseSessionId)
			.orElseThrow(() -> new BootSignalException(ErrorCode.BOOKMARK_NOT_FOUND));

		// 북마크 삭제
		bookmarkRepository.delete(bookmark);

		return new BookmarkDeleteResponse(courseSessionId);
	}

	/* 내 북마크 목록 조회 */
	public BookmarkPageResponse<BookmarkListResponse> getList(int page, int size, String sort) {
		validatePageParameters(page, size);
		BookmarkSort bookmarkSort = BookmarkSort.from(sort);
		User user = getAuthenticatedUser();

		// 북마크 목록 조회
		Pageable pageable = PageRequest.of(page, size, toSort(bookmarkSort));
		Page<Bookmark> bookmarkPage = bookmarkRepository.findAllByUserId(user.getId(), pageable);

		return BookmarkPageResponse.from(bookmarkPage.map(BookmarkListResponse::from));
	}

	// 페이지 유효성 검증
	private void validatePageParameters(int page, int size) {
		if (page < 0 || size <= 0) {
			throw new BootSignalException(ErrorCode.INVALID_PAGE_PARAMETER);
		}
	}

	// 정렬 조건 변환
	private Sort toSort(BookmarkSort bookmarkSort) {
		return switch (bookmarkSort) {
			case LATEST -> Sort.by(Sort.Direction.DESC, "startDate");
			case RATING -> Sort.by(Sort.Direction.DESC, "courseSession.course.stdgScor");
		};
	}


	// 인증된 사용자 조회
	private User getAuthenticatedUser() {
		String email = SecurityUtil.getCurrentUserEmail();
		return userRepository.findByEmail(email)
			.filter(u -> !u.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
	}

	// 과정 회차 조회
	private CourseSession getCourseSession(Long courseSessionId) {
		return courseSessionRepository.findById(courseSessionId)
			.orElseThrow(() -> new BootSignalException(ErrorCode.COURSE_SESSION_NOT_FOUND));
	}
}
