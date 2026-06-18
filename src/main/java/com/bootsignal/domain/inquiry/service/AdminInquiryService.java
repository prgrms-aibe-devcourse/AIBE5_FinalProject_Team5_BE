package com.bootsignal.domain.inquiry.service;

import com.bootsignal.domain.inquiry.dto.AdminInquiryResponse;
import com.bootsignal.domain.inquiry.dto.InquiryAnswerRequest;
import com.bootsignal.domain.inquiry.entity.Inquiry;
import com.bootsignal.domain.inquiry.entity.InquiryStatus;
import com.bootsignal.domain.inquiry.repository.InquiryRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.dto.PageResponse;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 관리자 문의 조회와 답변 등록/수정 처리를 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    public PageResponse<AdminInquiryResponse> getList(InquiryStatus status, Pageable pageable) {
        return PageResponse.from(
            inquiryRepository.findAdminList(status, pageable)
                .map(AdminInquiryResponse::from)
        );
    }

    public AdminInquiryResponse get(Long inquiryId) {
        return AdminInquiryResponse.from(findInquiry(inquiryId));
    }

    @Transactional
    public AdminInquiryResponse answer(Long inquiryId, InquiryAnswerRequest request) {
        Inquiry inquiry = findInquiry(inquiryId);
        User admin = getAuthenticatedUser();
        inquiry.answer(request.adminReply().trim(), admin, LocalDateTime.now());
        return AdminInquiryResponse.from(inquiry);
    }

    private Inquiry findInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND, "문의를 찾을 수 없습니다."));
    }

    private User getAuthenticatedUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
    }
}
