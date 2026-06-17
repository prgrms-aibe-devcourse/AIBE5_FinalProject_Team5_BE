package com.bootsignal.domain.inquiry.repository;

import com.bootsignal.domain.inquiry.entity.Inquiry;
import com.bootsignal.domain.inquiry.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 문의 목록, 상세, 관리자 상태 필터 조회를 담당하는 JPA 저장소입니다.
 */
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @EntityGraph(attributePaths = {"user", "answeredBy"})
    Page<Inquiry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "answeredBy"})
    Optional<Inquiry> findById(Long id);

    @Query(
        value = """
            SELECT i FROM Inquiry i
            JOIN FETCH i.user u
            LEFT JOIN FETCH i.answeredBy ab
            WHERE (:status IS NULL OR i.status = :status)
            """,
        countQuery = """
            SELECT count(i) FROM Inquiry i
            WHERE (:status IS NULL OR i.status = :status)
            """
    )
    Page<Inquiry> findAdminList(@Param("status") InquiryStatus status, Pageable pageable);
}
