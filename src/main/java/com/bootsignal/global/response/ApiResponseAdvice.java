package com.bootsignal.global.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestControllerAdvice(basePackages = "com.bootsignal")
@RequiredArgsConstructor
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

	private final ObjectMapper objectMapper;

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		Class<?> parameterType = returnType.getParameterType();
		// 파일, 바이트, 스트리밍 응답은 원본 응답 형식을 유지한다.
		return !ApiResponse.class.isAssignableFrom(parameterType)
			&& !ErrorResponse.class.isAssignableFrom(parameterType)
			&& !Resource.class.isAssignableFrom(parameterType)
			&& !byte[].class.isAssignableFrom(parameterType)
			&& !StreamingResponseBody.class.isAssignableFrom(parameterType);
	}

	@Override
	public Object beforeBodyWrite(
		Object body,
		MethodParameter returnType,
		MediaType selectedContentType,
		Class<? extends HttpMessageConverter<?>> selectedConverterType,
		ServerHttpRequest request,
		ServerHttpResponse response
	) {
		if (body instanceof ApiResponse<?> || isNoContent(response)) {
			return body;
		}

		if (body instanceof String) {
			// String 응답은 전용 컨버터가 처리하므로 JSON 문자열로 직접 직렬화한다.
			response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
			return writeAsString(ApiResponse.success(body));
		}

		if (!isJsonCompatible(selectedContentType) && body != null) {
			return body;
		}

		return ApiResponse.success(body);
	}

	private boolean isNoContent(ServerHttpResponse response) {
		if (response instanceof ServletServerHttpResponse servletResponse) {
			return servletResponse.getServletResponse().getStatus() == HttpStatus.NO_CONTENT.value();
		}
		return false;
	}

	private boolean isJsonCompatible(MediaType mediaType) {
		return mediaType == null
			|| MediaType.APPLICATION_JSON.includes(mediaType)
			|| mediaType.includes(MediaType.APPLICATION_JSON);
	}

	private String writeAsString(ApiResponse<?> response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("공통 응답 직렬화에 실패했습니다.", exception);
		}
	}
}
