package com.bootsignal.domain.inquiry.controller;

import com.bootsignal.domain.inquiry.dto.InquiryCreateRequest;
import com.bootsignal.domain.inquiry.dto.InquiryResponse;
import com.bootsignal.domain.inquiry.service.InquiryService;
import com.bootsignal.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 사용자의 1:1 문의 등록, 목록 조회, 상세 조회 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse create(@RequestBody @Valid InquiryCreateRequest request) {
        return inquiryService.create(request);
    }

    @GetMapping
    public PageResponse<InquiryResponse> getMyInquiries(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return inquiryService.getMyInquiries(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @GetMapping("/{inquiryId}")
    public InquiryResponse getMyInquiry(@PathVariable Long inquiryId) {
        return inquiryService.getMyInquiry(inquiryId);
    }
}
