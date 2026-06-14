package com.bootsignal.domain.bookmark.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.bookmark.dto.BookmarkCreateResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkDeleteResponse;
import com.bootsignal.domain.bookmark.service.BookmarkService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.time.LocalDateTime;
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
