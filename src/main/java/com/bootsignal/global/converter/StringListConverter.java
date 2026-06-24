package com.bootsignal.global.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class StringListConverter extends AbstractJsonListConverter<String> {

	private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

	@Override
	protected TypeReference<List<String>> typeReference() {
		return TYPE;
	}
}
