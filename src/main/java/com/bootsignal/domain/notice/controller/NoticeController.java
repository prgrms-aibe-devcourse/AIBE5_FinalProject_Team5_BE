package com.bootsignal.domain.notice.controller;

import com.bootsignal.domain.notice.dto.NoticeResponse;
import com.bootsignal.domain.notice.service.NoticeService;
import com.bootsignal.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비로그인 사용자도 접근할 수 있는 고객센터 공지 조회 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public PageResponse<NoticeResponse> getList(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return noticeService.getList(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @GetMapping("/{noticeId}")
    public NoticeResponse get(@PathVariable Long noticeId) {
        return noticeService.get(noticeId);
    }
}
