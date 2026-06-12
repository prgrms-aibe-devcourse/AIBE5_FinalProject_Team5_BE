package com.bootsignal.domain.ai.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftProject;
import com.bootsignal.domain.ai.portfolio.dto.PortfolioDraftResponse;
import com.bootsignal.domain.ai.portfolio.service.PortfolioDraftService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortfolioDraftController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PortfolioDraftController 테스트")
class PortfolioDraftControllerTest {

	// Controller 슬라이스에서는 요청 검증과 공통 응답 래핑만 확인한다.
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PortfolioDraftService portfolioDraftService;

	@Test
	@DisplayName("POST /api/ai/portfolio-drafts - 포트폴리오 초안 생성 성공")
	void createDraftReturnsOkResponse() throws Exception {
		UUID executionId = UUID.randomUUID();
		given(portfolioDraftService.createDraft(any()))
			.willReturn(new PortfolioDraftResponse(
				executionId,
				"백엔드 개발자로 성장 중인 지원자입니다.",
				List.of("Spring 기반 API 구현", "문제 해결 중심 개발"),
				List.of(new PortfolioDraftProject(
					"BootSignal",
					"교육 과정 탐색 서비스를 구현했습니다.",
					"백엔드 API 개발",
					List.of("Java", "Spring Boot"),
					List.of("JWT 인증과 게시글 API 구현")
				)),
				"Java와 Spring Boot를 중심으로 백엔드 API 구현 역량을 보유했습니다.",
				List.of("성과 수치를 추가하면 좋습니다.")
			));

		mockMvc.perform(post("/api/ai/portfolio-drafts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "targetJob": "백엔드 개발자",
					  "skills": ["Java", "Spring Boot"],
					  "projects": [
					    {
					      "name": "BootSignal",
					      "role": "백엔드 API 개발",
					      "description": "교육 과정 탐색 서비스를 구현했습니다.",
					      "techStack": ["Java", "Spring Boot"],
					      "achievement": "JWT 인증과 게시글 API를 구현했습니다."
					    }
					  ],
					  "education": "백엔드 부트캠프 수료",
					  "careerSummary": "팀 프로젝트 경험 보유",
					  "tone": "PROFESSIONAL"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.executionId").value(executionId.toString()))
			.andExpect(jsonPath("$.data.introduction").value("백엔드 개발자로 성장 중인 지원자입니다."))
			.andExpect(jsonPath("$.data.coreCompetencies[0]").value("Spring 기반 API 구현"))
			.andExpect(jsonPath("$.data.projectDescriptions[0].name").value("BootSignal"))
			.andExpect(jsonPath("$.data.techStackSummary").value("Java와 Spring Boot를 중심으로 백엔드 API 구현 역량을 보유했습니다."))
			.andExpect(jsonPath("$.error").doesNotExist());

		verify(portfolioDraftService).createDraft(any());
	}

	@Test
	@DisplayName("POST /api/ai/portfolio-drafts - 필수 입력 누락 시 400")
	void createDraftReturnsValidationErrorWhenRequiredFieldIsMissing() throws Exception {
		mockMvc.perform(post("/api/ai/portfolio-drafts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "targetJob": "",
					  "skills": [],
					  "projects": []
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		verify(portfolioDraftService, never()).createDraft(any());
	}
}
