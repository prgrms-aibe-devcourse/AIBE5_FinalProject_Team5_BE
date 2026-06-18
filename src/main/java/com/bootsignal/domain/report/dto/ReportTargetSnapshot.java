package com.bootsignal.domain.report.dto;

/**
 * 관리자 신고 화면에서 신고 대상 콘텐츠를 표시하기 위한 라벨, 본문, 이동 URL 묶음입니다.
 */
public record ReportTargetSnapshot(
    String targetLabel,
    String contentBody,
    String contentUrl
) {

    public static ReportTargetSnapshot missing(Long targetId) {
        return new ReportTargetSnapshot(
            "삭제된 신고 대상 #" + targetId,
            "신고 대상 콘텐츠를 찾을 수 없습니다.",
            "#"
        );
    }
}
