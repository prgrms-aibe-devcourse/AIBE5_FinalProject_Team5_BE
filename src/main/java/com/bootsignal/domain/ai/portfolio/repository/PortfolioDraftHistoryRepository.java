package com.bootsignal.domain.ai.portfolio.repository;

import com.bootsignal.domain.ai.portfolio.entity.PortfolioDraftHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioDraftHistoryRepository extends JpaRepository<PortfolioDraftHistory, Long> {

	Page<PortfolioDraftHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Optional<PortfolioDraftHistory> findByIdAndUserId(Long id, Long userId);
}
