package com.zerorisk.project.domain.ranking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// 주의: 총자산(currentAsset, initialAsset) 필드는 절대 추가하지 말 것.
// 전체 랭킹은 수익률(%)만 노출한다는 기획 원칙을 DTO 레벨에서 강제함.
public record RankingResponse(
        int rank,
        Long userId,
        String nickname,
        String userLevel,
        BigDecimal returnRate,
        LocalDate baseDate) { // 수익률 비교 기준 시작일 (ALL 기간은 기준일 개념이 없어 null)
}
