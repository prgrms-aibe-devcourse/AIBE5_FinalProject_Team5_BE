package com.bootsignal.domain.ai.openai;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.domain.ai.exception.AiRetryableException;
import com.bootsignal.global.config.properties.OpenAiProperties;
import com.bootsignal.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestClientOpenAiClient implements OpenAiClient {

	private final RestClient openAiRestClient;
	private final OpenAiProperties openAiProperties;
	private final ObjectMapper objectMapper;

	public RestClientOpenAiClient(
		@Qualifier("openAiRestClient") RestClient openAiRestClient,
		OpenAiProperties openAiProperties,
		ObjectMapper objectMapper
	) {
		this.openAiRestClient = openAiRestClient;
		this.openAiProperties = openAiProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	public OpenAiResponse complete(OpenAiRequest request) {
		if (!StringUtils.hasText(openAiProperties.apiKey())) {
			throw new AiNonRetryableException(ErrorCode.AI_EXECUTION_FAILED, "OpenAI API Key가 설정되지 않았습니다.");
		}

		try {
			JsonNode response = openAiRestClient.post()
				.uri("/responses")
				.body(toRequestBody(request))
				.retrieve()
				.body(JsonNode.class);
			return toOpenAiResponse(response);
		} catch (RestClientResponseException exception) {
			throw toAiException(exception);
		} catch (ResourceAccessException exception) {
			throw new AiRetryableException(
				ErrorCode.AI_EXECUTION_FAILED,
				"OpenAI API 호출 중 네트워크 오류가 발생했습니다.",
				exception
			);
		}
	}

	private Map<String, Object> toRequestBody(OpenAiRequest request) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", request.model());
		body.put("store", false);
		if (StringUtils.hasText(request.systemPrompt())) {
			body.put("instructions", request.systemPrompt());
		}
		body.put("input", request.userPrompt());

		Double temperature = request.temperature() != null ? request.temperature() : openAiProperties.temperature();
		if (temperature != null) {
			body.put("temperature", temperature);
		}

		Integer maxOutputTokens = request.maxOutputTokens() != null
			? request.maxOutputTokens()
			: openAiProperties.maxOutputTokens();
		if (maxOutputTokens != null) {
			body.put("max_output_tokens", maxOutputTokens);
		}
		return body;
	}

	private OpenAiResponse toOpenAiResponse(JsonNode response) {
		if (response == null || response.isNull()) {
			throw new AiRetryableException(ErrorCode.AI_OUTPUT_INVALID, "OpenAI 응답이 비어 있습니다.");
		}

		JsonNode error = response.path("error");
		if (!error.isMissingNode() && !error.isNull()) {
			throw new AiRetryableException(ErrorCode.AI_EXECUTION_FAILED, extractErrorMessage(response));
		}

		String content = extractContent(response);
		if (!StringUtils.hasText(content)) {
			throw new AiRetryableException(ErrorCode.AI_OUTPUT_INVALID, "OpenAI 응답 본문을 찾을 수 없습니다.");
		}

		JsonNode usage = response.path("usage");
		Integer promptTokens = integerOrNull(usage.path("input_tokens"));
		Integer completionTokens = integerOrNull(usage.path("output_tokens"));
		Integer totalTokens = integerOrNull(usage.path("total_tokens"));
		Integer reasoningTokens = integerOrNull(usage.path("output_tokens_details").path("reasoning_tokens"));

		return new OpenAiResponse(
			textOrNull(response.path("model")),
			content,
			promptTokens,
			completionTokens,
			totalTokens,
			reasoningTokens
		);
	}

	private String extractContent(JsonNode response) {
		String outputText = textOrNull(response.path("output_text"));
		if (StringUtils.hasText(outputText)) {
			return outputText;
		}

		List<String> texts = new ArrayList<>();
		for (JsonNode outputItem : response.path("output")) {
			for (JsonNode contentItem : outputItem.path("content")) {
				String text = textOrNull(contentItem.path("text"));
				if (StringUtils.hasText(text)) {
					texts.add(text);
				}
			}
		}
		return String.join("\n", texts).strip();
	}

	private RuntimeException toAiException(RestClientResponseException exception) {
		HttpStatusCode statusCode = exception.getStatusCode();
		String message = extractErrorMessage(exception.getResponseBodyAsString());
		if (isRetryable(statusCode)) {
			return new AiRetryableException(ErrorCode.AI_EXECUTION_FAILED, message, exception);
		}
		return new AiNonRetryableException(ErrorCode.AI_EXECUTION_FAILED, message, exception);
	}

	private boolean isRetryable(HttpStatusCode statusCode) {
		int value = statusCode.value();
		return value == 408 || value == 409 || value == 429 || statusCode.is5xxServerError();
	}

	private String extractErrorMessage(String responseBody) {
		if (!StringUtils.hasText(responseBody)) {
			return "OpenAI API 호출에 실패했습니다.";
		}
		try {
			return extractErrorMessage(objectMapper.readTree(responseBody));
		} catch (Exception exception) {
			return responseBody.strip();
		}
	}

	private String extractErrorMessage(JsonNode response) {
		String message = textOrNull(response.path("error").path("message"));
		return StringUtils.hasText(message) ? message : "OpenAI API 호출에 실패했습니다.";
	}

	private String textOrNull(JsonNode node) {
		return node != null && node.isTextual() ? node.asText() : null;
	}

	private Integer integerOrNull(JsonNode node) {
		return node != null && node.canConvertToInt() ? node.asInt() : null;
	}
}
