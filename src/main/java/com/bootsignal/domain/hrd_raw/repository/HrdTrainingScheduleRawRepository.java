package com.bootsignal.domain.hrd_raw.repository;

import com.bootsignal.domain.hrd_raw.entity.HrdTrainingScheduleRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrdTrainingScheduleRawRepository extends JpaRepository<HrdTrainingScheduleRaw, Long> {

    Optional<HrdTrainingScheduleRaw> findByTrprIdAndTrprDegr(String trprId, Integer trprDegr);
}
