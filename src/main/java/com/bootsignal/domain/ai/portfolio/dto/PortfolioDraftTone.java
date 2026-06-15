package com.bootsignal.domain.ai.portfolio.dto;

// 문체 옵션은 프롬프트에 설명 문장으로 전달해 응답 톤을 조정한다.
public enum PortfolioDraftTone {
	PROFESSIONAL("전문적이고 간결한 문체"),
	FRIENDLY("친근하지만 실무적인 문체"),
	CONCISE("짧고 핵심 중심의 문체");

	private final String description;

	PortfolioDraftTone(String description) {
		this.description = description;
	}

	public String description() {
		return description;
	}
}
