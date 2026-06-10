package com.bootsignal.domain.admin.service;

import com.bootsignal.domain.admin.dto.AdminNoticeCreateRequest;
import com.bootsignal.domain.admin.dto.AdminNoticeResponse;
import com.bootsignal.domain.notice.entity.Notice;
import com.bootsignal.domain.notice.repository.NoticeRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;

    @Transactional
    public AdminNoticeResponse send(AdminNoticeCreateRequest request) {
        User sender = getAuthenticatedUser();

        Notice notice = Notice.builder()
            .sender(sender)
            .title(request.title())
            .content(request.content())
            .build();

        return AdminNoticeResponse.from(noticeRepository.save(notice));
    }

    public Page<AdminNoticeResponse> getList(Pageable pageable) {
        return noticeRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(AdminNoticeResponse::from);
    }

    public AdminNoticeResponse get(Long noticeId) {
        return AdminNoticeResponse.from(findNotice(noticeId));
    }

    @Transactional
    public void delete(Long noticeId) {
        noticeRepository.delete(findNotice(noticeId));
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
    }

    private User getAuthenticatedUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
    }
}
