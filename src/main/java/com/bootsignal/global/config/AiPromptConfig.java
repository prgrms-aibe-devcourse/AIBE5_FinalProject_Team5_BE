package com.bootsignal.global.config;

import com.bootsignal.domain.ai.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiPromptConfig {

	// Agent별 프롬프트는 이름과 버전으로 등록해 실행 로그에서 추적 가능하게 한다.
	@Bean
	public PromptTemplate reviewSummaryPromptTemplate() {
		return new PromptTemplate(
			"REVIEW_SUMMARY",
			"v1",
			"""
				너는 부트캠프 리뷰를 분석하는 AI Agent다.
				과장된 표현을 줄이고, 제공된 리뷰 데이터 안에서만 요약한다.
				개인정보나 민감정보는 출력하지 않는다.
				""",
			"""
				다음 과정의 리뷰를 요약해줘.
				과정명: {{courseTitle}}
				리뷰 데이터:
				{{reviewContent}}

				출력 형식:
				- 핵심 장점
				- 아쉬운 점
				- 추천 대상
				"""
		);
	}

	@Bean
	public PromptTemplate portfolioDraftPromptTemplate() {
		return new PromptTemplate(
			"PORTFOLIO_DRAFT",
			"v1",
			"""
				너는 수료생의 포트폴리오 초안을 정리하는 AI Agent다.
				사용자가 제공한 경험과 기술만 사용하고, 사실로 확인되지 않은 성과를 만들어내지 않는다.
				""",
			"""
				다음 정보를 바탕으로 포트폴리오 초안을 작성해줘.
				목표 직무: {{targetJob}}
				기술 스택: {{skills}}
				프로젝트 경험:
				{{projectExperience}}

				출력 형식:
				- 소개 문장
				- 핵심 역량
				- 프로젝트 설명
				"""
		);
	}

	@Bean
	public PromptTemplate portfolioDraftPromptTemplateV2() {
		return new PromptTemplate(
			"PORTFOLIO_DRAFT",
			"v2",
			"""
				너는 취업 준비생의 포트폴리오 초안을 정리하는 AI Agent다.
				사용자가 제공한 경험과 기술만 사용하고, 확인되지 않은 수치 성과나 경력을 만들지 않는다.
				부족한 정보는 초안에 꾸며 넣지 말고 improvementSuggestions에 보완 질문으로 남긴다.
				응답은 반드시 JSON 객체만 출력하고, 마크다운 코드블록이나 설명 문장을 붙이지 않는다.
				""",
			"""
				다음 정보를 바탕으로 포트폴리오 초안을 작성해줘.

				목표 직무: {{targetJob}}
				희망 문체: {{tone}}
				기술 스택: {{skills}}
				교육/수료 정보: {{education}}
				경력 요약: {{careerSummary}}
				프로젝트 경험:
				{{projectExperience}}

				출력 JSON 스키마:
				{
				  "introduction": "목표 직무와 경험을 연결한 2문장 이내 소개",
				  "coreCompetencies": ["핵심 역량 1", "핵심 역량 2", "핵심 역량 3"],
				  "projectDescriptions": [
				    {
				      "name": "프로젝트명",
				      "summary": "프로젝트 설명 2~3문장",
				      "role": "담당 역할",
				      "techStack": ["사용 기술"],
				      "highlights": ["구현 또는 문제 해결 중심의 강조점"]
				    }
				  ],
				  "techStackSummary": "기술 스택을 직무 관점으로 정리한 문장",
				  "improvementSuggestions": ["추가하면 좋은 정보 또는 보완 질문"]
				}
				"""
		);
	}
}
