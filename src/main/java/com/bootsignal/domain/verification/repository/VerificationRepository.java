package com.bootsignal.domain.verification.repository;

import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    boolean existsByUserIdAndCourseSessionIdAndStatus(
        Long userId, Long courseSessionId, VerificationStatus status
    );
}
