package com.bootsignal.domain.bookmark.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.bookmark.dto.BookmarkCreateResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkDeleteResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkListResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkPageResponse;
import com.bootsignal.domain.bookmark.service.BookmarkService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookmarkController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BookmarkController 테스트")
class BookmarkControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BookmarkService bookmarkService;

	@Test
	@DisplayName("GET /api/bookmarks — 내 북마크 목록 조회 성공 (200)")
	void getListReturnsBookmarkPageResponse() throws Exception {
		BookmarkListResponse item = new BookmarkListResponse(
			1L,
			1L,
			LocalDateTime.of(2026, 6, 5, 10, 30),
			LocalDate.of(2026, 7, 1),
			LocalDate.of(2026, 12, 31),
			new BookmarkListResponse.BookmarkCourseSessionSummary(3, 30, 24, 20, 0),
			new BookmarkListResponse.BookmarkCourseSummary(10L, "백엔드 개발자 양성과정", BigDecimal.valueOf(4.7)),
			new BookmarkListResponse.BookmarkInstitutionSummary(
				5L, "한국소프트웨어교육원", "https://example.com/profile.png", "서울특별시 강남구"
			)
		);
		given(bookmarkService.getList(0, 20, "latest"))
			.willReturn(new BookmarkPageResponse<>(List.of(item), 0, 20, 1, 1, true));

		mockMvc.perform(get("/api/bookmarks")
				.queryParam("page", "0")
				.queryParam("size", "20")
				.queryParam("sort", "latest"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content[0].bookmarkId").value(1))
			.andExpect(jsonPath("$.data.content[0].courseSessionId").value(1))
			.andExpect(jsonPath("$.data.content[0].course.title").value("백엔드 개발자 양성과정"))
			.andExpect(jsonPath("$.data.content[0].institution.institutionName").value("한국소프트웨어교육원"))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(20))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.last").value(true))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("GET /api/bookmarks — 잘못된 정렬 값 (400)")
	void getListReturnsBadRequestWhenSortIsInvalid() throws Exception {
		given(bookmarkService.getList(0, 20, "invalid"))
			.willThrow(new BootSignalException(ErrorCode.INVALID_PAGE_PARAMETER));

		mockMvc.perform(get("/api/bookmarks")
				.queryParam("sort", "invalid"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INVALID_PAGE_PARAMETER"));
	}

	@Test
	@DisplayName("POST /api/bookmarks/courses/{courseSessionId} — 북마크 생성 성공 (201)")
	void createReturnsBookmarkCreateResponse() throws Exception {
		given(bookmarkService.create(10L)).willReturn(new BookmarkCreateResponse(
			1L,
			10L,
			LocalDateTime.of(2026, 6, 5, 10, 30)
		));

		mockMvc.perform(post("/api/bookmarks/courses/{courseSessionId}", 10L)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.bookmarkId").value(1))
			.andExpect(jsonPath("$.data.courseSessionId").value(10))
			.andExpect(jsonPath("$.data.createdAt").value("2026-06-05T10:30:00"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("POST /api/bookmarks/courses/{courseSessionId} — 존재하지 않는 회차 (404)")
	void createReturnsNotFoundWhenCourseSessionDoesNotExist() throws Exception {
		given(bookmarkService.create(999L))
			.willThrow(new BootSignalException(ErrorCode.COURSE_SESSION_NOT_FOUND));

		mockMvc.perform(post("/api/bookmarks/courses/{courseSessionId}", 999L)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("COURSE_SESSION_NOT_FOUND"));
	}

	@Test
	@DisplayName("POST /api/bookmarks/courses/{courseSessionId} — 이미 북마크한 회차 (409)")
	void createReturnsConflictWhenBookmarkAlreadyExists() throws Exception {
		given(bookmarkService.create(10L))
			.willThrow(new BootSignalException(ErrorCode.BOOKMARK_ALREADY_EXISTS));

		mockMvc.perform(post("/api/bookmarks/courses/{courseSessionId}", 10L)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("BOOKMARK_ALREADY_EXISTS"));
	}

	@Test
	@DisplayName("DELETE /api/bookmarks/courses/{courseSessionId} — 북마크 삭제 성공 (200)")
	void deleteReturnsBookmarkDeleteResponse() throws Exception {
		given(bookmarkService.delete(10L)).willReturn(new BookmarkDeleteResponse(10L));

		mockMvc.perform(delete("/api/bookmarks/courses/{courseSessionId}", 10L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.courseSessionId").value(10))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("DELETE /api/bookmarks/courses/{courseSessionId} — 북마크 없음 (404)")
	void deleteReturnsNotFoundWhenBookmarkDoesNotExist() throws Exception {
		given(bookmarkService.delete(10L))
			.willThrow(new BootSignalException(ErrorCode.BOOKMARK_NOT_FOUND));

		mockMvc.perform(delete("/api/bookmarks/courses/{courseSessionId}", 10L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("BOOKMARK_NOT_FOUND"));
	}
}
