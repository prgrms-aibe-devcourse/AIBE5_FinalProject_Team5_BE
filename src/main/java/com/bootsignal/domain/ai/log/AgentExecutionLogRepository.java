package com.bootsignal.domain.ai.log;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentExecutionLogRepository extends JpaRepository<AgentExecutionLog, Long> {

	Optional<AgentExecutionLog> findByExecutionId(String executionId);
}
