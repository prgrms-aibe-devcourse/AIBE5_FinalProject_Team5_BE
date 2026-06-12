package com.bootsignal.global.config;

import com.bootsignal.domain.ai.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiPromptConfig {

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
}
