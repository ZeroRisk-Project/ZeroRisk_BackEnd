package com.zerorisk.project.domain.ranking.service;

import com.zerorisk.project.domain.ranking.cache.RankingCacheService;
import com.zerorisk.project.domain.ranking.dto.RankingPeriod;
import com.zerorisk.project.domain.ranking.dto.RankingResponse;
import com.zerorisk.project.domain.ranking.dto.RankingRow;
import com.zerorisk.project.domain.ranking.repository.PortfolioSnapshotRankingRepository;
import com.zerorisk.project.global.exception.MyRankingNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final PortfolioSnapshotRankingRepository portfolioSnapshotRankingRepository;
    private final RankingCacheService rankingCacheService;

    // Scheduler 전용 - 무거운 연산이라 요청 시점마다 호출하면 안 됨.
    // SQL의 RANK()가 정렬+순위 부여까지 전부 끝낸 결과를 그대로 옮겨 담기만 한다.
    //
    // 스냅샷은 PortfolioSnapshotScheduler에 의해 평일(MON-FRI)에만 생성되므로, 달력일이 아니라
    // 직전 영업일 기준으로 기준일을 잡는다 (공휴일까지는 반영하지 못하고 주말만 제외하는 근사치).
    public void refreshRanking(RankingPeriod period) {
        LocalDate today = LocalDate.now();

        List<RankingRow> rows = switch (period) {
            case ALL -> portfolioSnapshotRankingRepository.findAllTimeRankings();
            case DAILY -> portfolioSnapshotRankingRepository.findPeriodRankings(today, previousBusinessDay(today, 1));
            case WEEKLY -> portfolioSnapshotRankingRepository.findPeriodRankings(today, previousBusinessDay(today, 5));
            case MONTHLY -> portfolioSnapshotRankingRepository.findPeriodRankings(today, previousBusinessDay(today, 21));
        };

        List<RankingResponse> rankings = rows.stream()
                .map(row -> new RankingResponse(row.rankPosition(), row.userId(), row.nickname(), row.userLevel(),
                        row.returnRate()))
                .toList();

        rankingCacheService.refresh(period, rankings);
    }

    // 주말만 제외한 근사 영업일 계산 (공휴일은 반영하지 않음)
    private LocalDate previousBusinessDay(LocalDate date, int businessDaysBack) {
        LocalDate result = date;
        int remaining = businessDaysBack;

        while (remaining > 0) {
            result = result.minusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                remaining--;
            }
        }

        return result;
    }

    public List<RankingResponse> getRankings(RankingPeriod period, int page, int size) {
        return rankingCacheService.getTopRankings(period, page, size);
    }

    public RankingResponse getMyRanking(RankingPeriod period, Long userId) {
        RankingResponse ranking = rankingCacheService.getMyRanking(period, userId);

        if (ranking == null) {
            throw new MyRankingNotFoundException();
        }

        return ranking;
    }
}
