package com.bootsignal.global.config;

import com.bootsignal.global.config.properties.OpenAiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {

	@Bean
	@Qualifier("openAiRestClient")
	public RestClient openAiRestClient(RestClient.Builder builder, OpenAiProperties openAiProperties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(openAiProperties.timeoutMillis());
		requestFactory.setReadTimeout(openAiProperties.timeoutMillis());

		RestClient.Builder openAiBuilder = builder
			.baseUrl("https://api.openai.com/v1")
			.requestFactory(requestFactory)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		if (StringUtils.hasText(openAiProperties.apiKey())) {
			openAiBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.apiKey());
		}
		return openAiBuilder.build();
	}
}
