package com.bootsignal.domain.review.entity;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Arrays;

/**
 * 인증 리뷰 설문 enum의 JSON 입력값을 value, enum 이름, 한글 라벨 기준으로 변환하는 공통 파서입니다.
 */
final class ReviewSurveyOptionParser {

    private ReviewSurveyOptionParser() {
    }

    static <T extends Enum<T> & ReviewSurveyOption> T parse(T[] values, String value, String errorMessage) {
        if (value == null) {
            throw new BootSignalException(ErrorCode.BAD_REQUEST, errorMessage);
        }

        return Arrays.stream(values)
            .filter(item -> item.value().equalsIgnoreCase(value)
                || item.name().equalsIgnoreCase(value)
                || item.label().equals(value))
            .findFirst()
            .orElseThrow(() -> new BootSignalException(ErrorCode.BAD_REQUEST, errorMessage));
    }
}
