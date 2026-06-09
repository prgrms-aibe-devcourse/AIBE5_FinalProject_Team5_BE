package com.bootsignal.domain.ai.openai;

public interface OpenAiClient {

	OpenAiResponse complete(OpenAiRequest request);
}
