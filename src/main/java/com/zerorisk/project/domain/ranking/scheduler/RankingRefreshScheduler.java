package com.zerorisk.project.domain.ranking.scheduler;

import com.zerorisk.project.domain.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// TODO: 오상민님의 일별 자산 스냅샷 배치(장 마감 후 실행) 완료 시각 확인 후, cron을 그 이후 시간으로 맞출 것.
// 지금은 임시로 매일 16:00(장 마감 15:30 이후 30분 여유)로 설정.
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingRefreshScheduler {

    private final RankingService rankingService;

    @Scheduled(cron = "0 0 16 * * *")
    public void refresh() {
        log.info("랭킹 캐시 갱신 시작");

        rankingService.refreshRanking();

        log.info("랭킹 캐시 갱신 완료");
    }
}