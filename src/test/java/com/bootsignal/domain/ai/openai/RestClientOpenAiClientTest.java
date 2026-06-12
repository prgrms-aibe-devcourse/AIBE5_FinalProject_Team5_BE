package com.bootsignal.domain.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bootsignal.domain.ai.exception.AiNonRetryableException;
import com.bootsignal.domain.ai.exception.AiRetryableException;
import com.bootsignal.global.config.properties.OpenAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestClientOpenAiClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void completeCallsResponsesApiAndParsesUsage() {
		RestClient.Builder builder = RestClient.builder()
			.baseUrl("https://api.openai.com/v1")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestClientOpenAiClient client = new RestClientOpenAiClient(
			builder.build(),
			new OpenAiProperties("test-key", "gpt-4o", 30_000, null, null),
			objectMapper
		);

		server.expect(requestTo("https://api.openai.com/v1/responses"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
			.andExpect(jsonPath("$.model").value("gpt-4o"))
			.andExpect(jsonPath("$.store").value(false))
			.andExpect(jsonPath("$.instructions").value("시스템 프롬프트"))
			.andExpect(jsonPath("$.input").value("사용자 프롬프트"))
			.andRespond(withSuccess("""
				{
				  "model": "gpt-4o",
				  "output_text": "요약 결과입니다.",
				  "usage": {
				    "input_tokens": 12,
				    "output_tokens": 8,
				    "total_tokens": 20,
				    "output_tokens_details": {
				      "reasoning_tokens": 3
				    }
				  }
				}
				""", MediaType.APPLICATION_JSON));

		OpenAiResponse response = client.complete(new OpenAiRequest(
			"gpt-4o",
			"시스템 프롬프트",
			"사용자 프롬프트",
			"REVIEW_SUMMARY:v1",
			null,
			null
		));

		assertThat(response.content()).isEqualTo("요약 결과입니다.");
		assertThat(response.promptTokens()).isEqualTo(12);
		assertThat(response.completionTokens()).isEqualTo(8);
		assertThat(response.totalTokens()).isEqualTo(20);
		assertThat(response.reasoningTokens()).isEqualTo(3);
		server.verify();
	}

	@Test
	void completeThrowsRetryableExceptionOnRateLimit() {
		RestClient.Builder builder = RestClient.builder()
			.baseUrl("https://api.openai.com/v1")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestClientOpenAiClient client = new RestClientOpenAiClient(
			builder.build(),
			new OpenAiProperties("test-key", "gpt-4o", 30_000, null, null),
			objectMapper
		);

		server.expect(requestTo("https://api.openai.com/v1/responses"))
			.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
					{
					  "error": {
					    "message": "rate limit"
					  }
					}
					"""));

		assertThatThrownBy(() -> client.complete(new OpenAiRequest("gpt-4o", "", "질문")))
			.isInstanceOf(AiRetryableException.class)
			.hasMessage("rate limit");
		server.verify();
	}

	@Test
	void completeThrowsNonRetryableExceptionOnUnauthorized() {
		RestClient.Builder builder = RestClient.builder()
			.baseUrl("https://api.openai.com/v1")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestClientOpenAiClient client = new RestClientOpenAiClient(
			builder.build(),
			new OpenAiProperties("test-key", "gpt-4o", 30_000, null, null),
			objectMapper
		);

		server.expect(requestTo("https://api.openai.com/v1/responses"))
			.andRespond(withStatus(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
					{
					  "error": {
					    "message": "invalid api key"
					  }
					}
					"""));

		assertThatThrownBy(() -> client.complete(new OpenAiRequest("gpt-4o", "", "질문")))
			.isInstanceOf(AiNonRetryableException.class)
			.hasMessage("invalid api key");
		server.verify();
	}

	@Test
	void completeStopsBeforeHttpCallWhenApiKeyIsMissing() {
		RestClientOpenAiClient client = new RestClientOpenAiClient(
			RestClient.builder().baseUrl("https://api.openai.com/v1").build(),
			new OpenAiProperties("", "gpt-4o", 30_000, null, null),
			objectMapper
		);

		assertThatThrownBy(() -> client.complete(new OpenAiRequest("gpt-4o", "", "질문")))
			.isInstanceOf(AiNonRetryableException.class)
			.hasMessage("OpenAI API Key가 설정되지 않았습니다.");
	}
}
