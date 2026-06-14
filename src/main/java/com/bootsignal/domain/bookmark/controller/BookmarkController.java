package com.bootsignal.domain.bookmark.controller;

import com.bootsignal.domain.bookmark.dto.BookmarkCreateResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkDeleteResponse;
import com.bootsignal.domain.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

	private final BookmarkService bookmarkService;

	/* 북마크 생성 */
	@PostMapping("/courses/{courseSessionId}")
	@ResponseStatus(HttpStatus.CREATED)
	public BookmarkCreateResponse create(@PathVariable Long courseSessionId) {
		return bookmarkService.create(courseSessionId);
	}

	/* 북마크 삭제 */
	@DeleteMapping("/courses/{courseSessionId}")
	public BookmarkDeleteResponse delete(@PathVariable Long courseSessionId) {
		return bookmarkService.delete(courseSessionId);
	}
}
