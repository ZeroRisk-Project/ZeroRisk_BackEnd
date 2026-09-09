package com.zerorisk.project.domain.stock.client.kis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// 앱 기동 후 첫 시세/차트 조회 요청이 토큰 발급까지 함께 기다리지 않도록,
// 기동 시점에 미리 KIS 액세스 토큰을 발급받아 캐시를 채워둔다.
@Slf4j
@Component
@RequiredArgsConstructor
public class KisTokenWarmupRunner {

    private final KisTokenService kisTokenService;
    private final KisAccessTokenProvider kisRankingTokenProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        try {
            kisTokenService.getAccessToken();
            log.info("KIS 시세조회용 액세스 토큰 워밍업 완료");
        } catch (Exception e) {
            log.warn("KIS 시세조회용 액세스 토큰 워밍업 실패", e);
        }

        try {
            kisRankingTokenProvider.getAccessToken();
            log.info("KIS 랭킹조회용 액세스 토큰 워밍업 완료");
        } catch (Exception e) {
            log.warn("KIS 랭킹조회용 액세스 토큰 워밍업 실패", e);
        }
    }
}
