package com.bootsignal.domain.ai.portfolio.converter;

import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftProject;
import com.bootsignal.global.converter.AbstractJsonListConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class PortfolioDraftProjectListConverter extends AbstractJsonListConverter<PortfolioDraftProject> {

	private static final TypeReference<List<PortfolioDraftProject>> TYPE = new TypeReference<>() {};

	@Override
	protected TypeReference<List<PortfolioDraftProject>> typeReference() {
		return TYPE;
	}
}
