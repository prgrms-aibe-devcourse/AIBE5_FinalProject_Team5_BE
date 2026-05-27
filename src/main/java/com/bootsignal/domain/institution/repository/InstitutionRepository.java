package com.bootsignal.domain.institution.repository;

import com.bootsignal.domain.institution.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByInstCd(String instCd);
}
