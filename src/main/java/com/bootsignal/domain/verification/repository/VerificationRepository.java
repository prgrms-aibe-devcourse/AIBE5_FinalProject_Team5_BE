package com.bootsignal.domain.verification.repository;

import com.bootsignal.domain.verification.entity.Verification;
import com.bootsignal.domain.verification.entity.VerificationStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 인증 신청의 중복 여부, 상태별 목록, 상세 조회를 담당하는 저장소입니다.
 */
public interface VerificationRepository extends JpaRepository<Verification, Long> {

    boolean existsByUserIdAndCourseSessionIdAndStatus(
        Long userId, Long courseSessionId, VerificationStatus status
    );

    boolean existsByUserIdAndCourseSessionId(Long userId, Long courseSessionId);

    long countByStatus(VerificationStatus status);

    @EntityGraph(attributePaths = {"user", "course", "courseSession", "processedBy"})
    @Query("""
        SELECT v FROM Verification v
        WHERE v.user.id = :userId
          AND (:status IS NULL OR v.status = :status)
        """)
    Page<Verification> findMyVerifications(
        @Param("userId") Long userId,
        @Param("status") VerificationStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "course", "courseSession", "processedBy"})
    Optional<Verification> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"user", "course", "courseSession", "processedBy"})
    @Query("""
        SELECT v FROM Verification v
        WHERE (:status IS NULL OR v.status = :status)
        """)
    Page<Verification> findAdminVerifications(
        @Param("status") VerificationStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "course", "courseSession", "processedBy"})
    @Query("SELECT v FROM Verification v WHERE v.id = :id")
    Optional<Verification> findWithDetailsById(@Param("id") Long id);
}
