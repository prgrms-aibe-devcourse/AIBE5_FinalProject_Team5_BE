package com.bootsignal.domain.course.dto;

/**
 * 분야 카테고리 필터 (NCS 코드 기반 분류)
 * - AI        : 인공지능 관련 NCS 코드
 * - SECURITY  : 정보보호/보안 관련 NCS 코드
 * - BIG_DATA  : 빅데이터 관련 NCS 코드
 * - CLOUD     : 클라우드 관련 NCS 코드
 * - UI_UX     : UI/UX 관련 NCS 코드
 * - VR        : 가상현실/증강현실 관련 NCS 코드
 * - APP_SW    : 응용SW 개발 관련 NCS 코드
 * - OTHERS    : 위 7개 카테고리에 속하지 않는 나머지
 */
public enum FieldCategory {
    AI,
    SECURITY,
    BIG_DATA,
    CLOUD,
    UI_UX,
    VR,
    APP_SW,
    OTHERS
}
