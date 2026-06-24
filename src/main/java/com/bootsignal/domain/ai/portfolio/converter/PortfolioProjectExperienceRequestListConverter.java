package com.bootsignal.domain.ai.portfolio.converter;

import com.bootsignal.domain.ai.portfolio.dto.PortfolioProjectExperienceRequest;
import com.bootsignal.global.converter.AbstractJsonListConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class PortfolioProjectExperienceRequestListConverter extends AbstractJsonListConverter<PortfolioProjectExperienceRequest> {

	private static final TypeReference<List<PortfolioProjectExperienceRequest>> TYPE = new TypeReference<>() {};

	@Override
	protected TypeReference<List<PortfolioProjectExperienceRequest>> typeReference() {
		return TYPE;
	}
}
