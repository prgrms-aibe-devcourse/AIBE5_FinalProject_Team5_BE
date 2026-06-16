package com.bootsignal.domain.review.entity;

/**
 * 인증 리뷰 설문 enum이 프론트 입력값과 표시 라벨을 공통으로 제공하도록 맞추는 인터페이스입니다.
 */
interface ReviewSurveyOption {

    String value();

    String label();
}
