package com.bootsignal.domain.institution.dto;

import com.bootsignal.domain.institution.entity.Institution;

import java.time.LocalDateTime;

public record InstitutionResponse(
        Long id,
        String instCd,
        String institutionName,
        String address,
        String homepageUrl,
        String managerName,
        String managerTel,
        String managerEmail,
        String profileImageUrl,
        String introduction,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InstitutionResponse from(Institution institution) {
        return new InstitutionResponse(
                institution.getId(),
                institution.getInstCd(),
                institution.getInstitutionName(),
                institution.getAddress(),
                institution.getHomepageUrl(),
                institution.getManagerName(),
                institution.getManagerTel(),
                institution.getManagerEmail(),
                institution.getProfileImageUrl(),
                institution.getIntroduction(),
                institution.getCreatedAt(),
                institution.getUpdatedAt()
        );
    }
}
