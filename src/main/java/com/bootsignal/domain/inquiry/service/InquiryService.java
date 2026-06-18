package com.bootsignal.domain.inquiry.service;

import com.bootsignal.domain.inquiry.dto.InquiryCreateRequest;
import com.bootsignal.domain.inquiry.dto.InquiryResponse;
import com.bootsignal.domain.inquiry.entity.Inquiry;
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

/**
 * 로그인 사용자의 문의 등록과 본인 문의 조회 권한 검증을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @Transactional
    public InquiryResponse create(InquiryCreateRequest request) {
        User user = getAuthenticatedUser();
        Inquiry inquiry = Inquiry.builder()
            .user(user)
            .title(request.title().trim())
            .content(request.content().trim())
            .build();
        return InquiryResponse.from(inquiryRepository.save(inquiry));
    }

    public PageResponse<InquiryResponse> getMyInquiries(Pageable pageable) {
        User user = getAuthenticatedUser();
        return PageResponse.from(
            inquiryRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(InquiryResponse::from)
        );
    }

    public InquiryResponse getMyInquiry(Long inquiryId) {
        User user = getAuthenticatedUser();
        Inquiry inquiry = findInquiry(inquiryId);
        if (!inquiry.isOwnedBy(user.getId())) {
            throw new BootSignalException(ErrorCode.FORBIDDEN);
        }
        return InquiryResponse.from(inquiry);
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
