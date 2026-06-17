package com.bootsignal.domain.notice.service;

import com.bootsignal.domain.notice.dto.NoticeResponse;
import com.bootsignal.domain.notice.entity.Notice;
import com.bootsignal.domain.notice.repository.NoticeRepository;
import com.bootsignal.global.dto.PageResponse;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고객센터에서 노출할 공개 공지 목록과 상세 조회를 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public PageResponse<NoticeResponse> getList(Pageable pageable) {
        return PageResponse.from(
            noticeRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(NoticeResponse::from)
        );
    }

    public NoticeResponse get(Long noticeId) {
        return NoticeResponse.from(findNotice(noticeId));
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
    }
}
