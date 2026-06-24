package com.bootsignal.global.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import java.util.List;

public abstract class AbstractJsonListConverter<T> implements AttributeConverter<List<T>, String> {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	protected abstract TypeReference<List<T>> typeReference();

	@Override
	public String convertToDatabaseColumn(List<T> attribute) {
		if (attribute == null) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("JSON 직렬화 실패", e);
		}
	}

	@Override
	public List<T> convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return List.of();
		}
		try {
			return MAPPER.readValue(dbData, typeReference());
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("JSON 역직렬화 실패", e);
		}
	}
}
