package com.bootsignal.domain.ai.openai;

public interface OpenAiClient {

	// 외부 LLM 호출은 이 인터페이스 뒤로 숨겨 Controller와 Service의 직접 호출을 막는다.
	OpenAiResponse complete(OpenAiRequest request);
}
