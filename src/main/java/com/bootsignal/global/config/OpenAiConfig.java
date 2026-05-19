package com.bootsignal.global.config;

import com.bootsignal.global.config.properties.OpenAiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {

	@Bean
	public RestClient openAiRestClient(RestClient.Builder builder, OpenAiProperties openAiProperties) {
		RestClient.Builder openAiBuilder = builder.baseUrl("https://api.openai.com/v1");
		if (StringUtils.hasText(openAiProperties.apiKey())) {
			openAiBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.apiKey());
		}
		return openAiBuilder.build();
	}
}

