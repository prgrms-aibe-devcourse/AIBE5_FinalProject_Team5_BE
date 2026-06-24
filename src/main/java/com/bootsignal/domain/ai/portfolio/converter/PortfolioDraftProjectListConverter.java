package com.bootsignal.domain.ai.portfolio.converter;

import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftProject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class PortfolioDraftProjectListConverter
	implements AttributeConverter<List<PortfolioDraftProject>, String> {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final TypeReference<List<PortfolioDraftProject>> TYPE = new TypeReference<>() {};

	@Override
	public String convertToDatabaseColumn(List<PortfolioDraftProject> attribute) {
		if (attribute == null) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("List<PortfolioDraftProject> 직렬화 실패", e);
		}
	}

	@Override
	public List<PortfolioDraftProject> convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return List.of();
		}
		try {
			return MAPPER.readValue(dbData, TYPE);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("List<PortfolioDraftProject> 역직렬화 실패", e);
		}
	}
}
