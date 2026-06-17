package com.bootsignal.domain.inquiry.controller;

import com.bootsignal.domain.inquiry.dto.AdminInquiryResponse;
import com.bootsignal.domain.inquiry.dto.InquiryAnswerRequest;
import com.bootsignal.domain.inquiry.entity.InquiryStatus;
import com.bootsignal.domain.inquiry.service.AdminInquiryService;
import com.bootsignal.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자의 문의 목록/상세 조회와 답변 등록 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @GetMapping
    public PageResponse<AdminInquiryResponse> getList(
        @RequestParam(required = false) InquiryStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return adminInquiryService.getList(
            status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @GetMapping("/{inquiryId}")
    public AdminInquiryResponse get(@PathVariable Long inquiryId) {
        return adminInquiryService.get(inquiryId);
    }

    @PatchMapping("/{inquiryId}/answer")
    public AdminInquiryResponse answer(
        @PathVariable Long inquiryId,
        @RequestBody @Valid InquiryAnswerRequest request
    ) {
        return adminInquiryService.answer(inquiryId, request);
    }
}
