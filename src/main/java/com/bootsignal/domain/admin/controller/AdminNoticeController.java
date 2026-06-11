package com.bootsignal.domain.admin.controller;

import com.bootsignal.domain.admin.dto.AdminNoticeCreateRequest;
import com.bootsignal.domain.admin.dto.AdminNoticeResponse;
import com.bootsignal.domain.admin.service.AdminNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    /** 공지 발송 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminNoticeResponse send(@RequestBody @Valid AdminNoticeCreateRequest request) {
        return adminNoticeService.send(request);
    }

    /** 발송 내역 목록 조회 */
    @GetMapping
    public Page<AdminNoticeResponse> getList(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return adminNoticeService.getList(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /** 공지 상세 조회 */
    @GetMapping("/{noticeId}")
    public AdminNoticeResponse get(@PathVariable Long noticeId) {
        return adminNoticeService.get(noticeId);
    }

    /** 공지 삭제 */
    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long noticeId) {
        adminNoticeService.delete(noticeId);
    }
}
