package com.bootsignal.domain.institution.service;

import com.bootsignal.domain.institution.dto.InstitutionResponse;
import com.bootsignal.domain.institution.entity.Institution;
import com.bootsignal.domain.institution.repository.InstitutionRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public InstitutionResponse getInstitution(Long institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new BootSignalException(ErrorCode.INSTITUTION_NOT_FOUND));
        return InstitutionResponse.from(institution);
    }
}
