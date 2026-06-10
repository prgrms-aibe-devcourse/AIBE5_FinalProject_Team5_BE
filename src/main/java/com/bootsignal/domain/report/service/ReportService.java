package com.bootsignal.domain.report.service;

import com.bootsignal.domain.report.dto.ReportCreateRequest;
import com.bootsignal.domain.report.dto.ReportResponse;
import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.repository.ReportRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReportResponse create(ReportCreateRequest request) {
        User reporter = getAuthenticatedUser();

        Report report = Report.builder()
            .reporter(reporter)
            .targetType(request.targetType())
            .targetId(request.targetId())
            .reason(request.reason())
            .detail(request.detail())
            .build();

        return ReportResponse.from(reportRepository.save(report));
    }

    private User getAuthenticatedUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
    }
}
