package com.zerorisk.project.domain.ranking.scheduler;

import com.zerorisk.project.domain.ranking.dto.RankingPeriod;
import com.zerorisk.project.domain.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 오상민님의 PortfolioSnapshotScheduler(매일 16:00, 스냅샷 생성)가 먼저 끝나야 하므로
// 랭킹 갱신은 그보다 10분 뒤인 16:10으로 설정. 스냅샷은 평일에만 생성되므로 동일하게 MON-FRI로 맞춤.
// app.scheduling.enabled=false(벤치마크 프로필 등)일 때만 비활성화 - 값이 없으면(matchIfMissing)
// 기존과 동일하게 항상 켜짐. 실서비스 등 다른 프로필엔 영향 없음.
@Slf4j
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RankingRefreshScheduler {

    private final RankingService rankingService;

    @Scheduled(cron = "0 10 16 * * MON-FRI", zone = "Asia/Seoul")
    public void refreshAll() {
        log.info("랭킹 캐시 갱신 시작");

        for (RankingPeriod period : RankingPeriod.values()) {
            try {
                rankingService.refreshRanking(period);
            } catch (Exception e) {
                // 한 기간(period)의 갱신이 실패해도 나머지 기간은 계속 갱신한다 - 여기서 멈추면
                // 예외 없이 조용히 나머지 캐시가 그날 내내 stale 상태로 남는다.
                log.error("랭킹 캐시 갱신 실패 - period: {}", period, e);
            }
        }

        log.info("랭킹 캐시 갱신 완료");
    }
}
