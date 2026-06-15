package com.bootsignal.domain.ai.portfolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.ai.log.AgentExecutionLog;
import com.bootsignal.domain.ai.log.AgentExecutionLogRepository;
import com.bootsignal.domain.ai.log.AgentExecutionStatus;
import com.bootsignal.domain.ai.openai.OpenAiClient;
import com.bootsignal.domain.ai.openai.OpenAiRequest;
import com.bootsignal.domain.ai.openai.OpenAiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortfolioDraftIntegrationTest {

	// 회원가입부터 JWT 인증 API 호출, Agent 실행 로그 저장까지 실제 웹 흐름으로 검증한다.
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AgentExecutionLogRepository logRepository;

	@MockitoBean
	private OpenAiClient openAiClient;

	@Test
	void createPortfolioDraftWithLoginAccessToken() throws Exception {
		given(openAiClient.complete(any()))
			.willReturn(new OpenAiResponse("gpt-4o", validPortfolioDraftJson(), 100, 200, 300, null));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "portfolio-e2e@example.com",
					  "password": "password123",
					  "nickname": "portfolioUser"
					}
					"""))
			.andExpect(status().isCreated());

		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "portfolio-e2e@example.com",
					  "password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		String accessToken = objectMapper.readTree(loginResponse)
			.path("data")
			.path("accessToken")
			.asText();

		String draftResponse = mockMvc.perform(post("/api/ai/portfolio-drafts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "targetJob": "백엔드 개발자",
					  "skills": ["Java", "Spring Boot", "JPA"],
					  "projects": [
					    {
					      "name": "BootSignal",
					      "role": "백엔드 API 개발",
					      "description": "국비 교육 과정 탐색과 커뮤니티 기능을 제공하는 서비스를 구현했습니다.",
					      "techStack": ["Java", "Spring Boot", "JPA"],
					      "achievement": "JWT 인증과 포트폴리오 초안 생성 API를 구현했습니다."
					    }
					  ],
					  "education": "백엔드 부트캠프 수료",
					  "careerSummary": "팀 프로젝트 기반 백엔드 개발 경험 보유",
					  "tone": "PROFESSIONAL"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.introduction").value("백엔드 개발자를 목표로 교육 과정 탐색 서비스를 구현한 지원자입니다."))
			.andExpect(jsonPath("$.data.coreCompetencies[0]").value("Spring 기반 API 구현"))
			.andExpect(jsonPath("$.data.projectDescriptions[0].name").value("BootSignal"))
			.andExpect(jsonPath("$.data.techStackSummary").value("Java와 Spring Boot를 활용해 백엔드 API를 구현할 수 있습니다."))
			.andExpect(jsonPath("$.error").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		JsonNode data = objectMapper.readTree(draftResponse).path("data");
		UUID executionId = UUID.fromString(data.path("executionId").asText());
		AgentExecutionLog log = logRepository.findByExecutionId(executionId.toString()).orElseThrow();

		assertThat(log.getStatus()).isEqualTo(AgentExecutionStatus.SUCCESS);
		assertThat(log.getPromptVersion()).isEqualTo("PORTFOLIO_DRAFT:v2");
		assertThat(log.getModel()).isEqualTo("gpt-4o");
		assertThat(log.getTotalTokens()).isEqualTo(300);

		ArgumentCaptor<OpenAiRequest> captor = ArgumentCaptor.forClass(OpenAiRequest.class);
		verify(openAiClient).complete(captor.capture());
		assertThat(captor.getValue().promptVersion()).isEqualTo("PORTFOLIO_DRAFT:v2");
		assertThat(captor.getValue().userPrompt()).contains("목표 직무: 백엔드 개발자");
	}

	private String validPortfolioDraftJson() {
		return """
			{
			  "introduction": "백엔드 개발자를 목표로 교육 과정 탐색 서비스를 구현한 지원자입니다.",
			  "coreCompetencies": ["Spring 기반 API 구현", "JPA 기반 데이터 모델링"],
			  "projectDescriptions": [
			    {
			      "name": "BootSignal",
			      "summary": "국비 교육 과정 탐색과 커뮤니티 기능을 제공하는 서비스를 구현했습니다.",
			      "role": "백엔드 API 개발",
			      "techStack": ["Java", "Spring Boot", "JPA"],
			      "highlights": ["JWT 인증을 구현했습니다.", "포트폴리오 초안 생성 API를 구현했습니다."]
			    }
			  ],
			  "techStackSummary": "Java와 Spring Boot를 활용해 백엔드 API를 구현할 수 있습니다.",
			  "improvementSuggestions": ["프로젝트 성과 수치를 추가하면 설득력이 높아집니다."]
			}
			""";
	}
}
