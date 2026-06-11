package com.bootsignal.domain.admin.service;

import com.bootsignal.domain.admin.dto.AdminDashboardSummaryResponse;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.report.repository.ReportRepository;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import com.bootsignal.domain.verification.repository.VerificationRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final VerificationRepository verificationRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final JobExplorer jobExplorer;

    public AdminDashboardSummaryResponse getSummary() {
        return new AdminDashboardSummaryResponse(
            userRepository.count(),
            courseRepository.count(),
            verificationRepository.countByStatus(VerificationStatus.PENDING),
            reviewRepository.count(),
            reportRepository.count(),
            getLastJobEndTime("hrdDataCollectJob"),
            getLastJobEndTime("hrdDataRefineJob")
        );
    }

    private LocalDateTime getLastJobEndTime(String jobName) {
        return jobExplorer.getJobInstances(jobName, 0, 1).stream()
            .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
            .filter(execution -> execution.getEndTime() != null)
            .map(execution -> execution.getEndTime()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime())
            .findFirst()
            .orElse(null);
    }
}
