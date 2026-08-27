package com.zerorisk.project.domain.ranking.dto;

// 내부 계산용 DTO. 계좌 ID -> 유저 정보 매핑에만 사용 (API 응답으로 직접 나가지 않음)
public record AccountUserInfoRow(
        Long accountId,
        Long userId,
        String nickname,
        String userLevel) {
}
