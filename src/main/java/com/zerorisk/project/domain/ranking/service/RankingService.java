package com.zerorisk.project.domain.ranking.service;

import com.zerorisk.project.domain.portfolio.entity.PortfolioSnapshot;
import com.zerorisk.project.domain.portfolio.repository.PortfolioSnapshotRepository;
import com.zerorisk.project.domain.ranking.cache.RankingCacheService;
import com.zerorisk.project.domain.ranking.dto.AccountReturnRateRow;
import com.zerorisk.project.domain.ranking.dto.AccountUserInfoRow;
import com.zerorisk.project.domain.ranking.dto.RankingPeriod;
import com.zerorisk.project.domain.ranking.dto.RankingResponse;
import com.zerorisk.project.domain.ranking.repository.PortfolioSnapshotRankingRepository;
import com.zerorisk.project.global.exception.MyRankingNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final PortfolioSnapshotRankingRepository portfolioSnapshotRankingRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final RankingCacheService rankingCacheService;

    // Scheduler 전용 - 무거운 연산이라 요청 시점마다 호출하면 안 됨
    // "전체 누적" 랭킹 - 초기 시드머니 대비 누적 수익률 기준
    public void refreshRanking() {
        List<AccountReturnRateRow> rows = portfolioSnapshotRankingRepository.findAllReturnRates();

        List<RankingResponse> sorted = rows.stream()
                .map(row -> RankingResponse.of(0, row))
                .sorted(Comparator.comparing(RankingResponse::returnRate).reversed())
                .toList();

        List<RankingResponse> ranked = assignRanks(sorted);

        rankingCacheService.refresh(RankingPeriod.ALL, ranked);
    }

    // Scheduler 전용 - 기간별(일간/주간/월간) 랭킹.
    // ALL은 위 refreshRanking()의 "전체 누적" 로직을 그대로 재사용함(계산 기준 자체가 다름 - 초기 시드머니 대비 누적).
    //
    // 스냅샷은 PortfolioSnapshotScheduler에 의해 평일(MON-FRI)에만 생성되므로, 달력일이 아니라
    // 직전 영업일 기준으로 기준일을 잡는다 (공휴일까지는 반영하지 못하고 주말만 제외하는 근사치).
    public void refreshRanking(RankingPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate baseDate = switch (period) {
            case DAILY -> previousBusinessDay(today, 1);
            case WEEKLY -> previousBusinessDay(today, 5);
            case MONTHLY -> previousBusinessDay(today, 21);
            case ALL -> null;
        };

        if (baseDate == null) {
            refreshRanking();
            return;
        }

        Map<Long, PortfolioSnapshot> baseSnapshots = portfolioSnapshotRepository.findAllBySnapshotDate(baseDate).stream()
                .collect(Collectors.toMap(PortfolioSnapshot::getAccountId, Function.identity()));

        Map<Long, PortfolioSnapshot> latestSnapshots = portfolioSnapshotRepository.findAllBySnapshotDate(today).stream()
                .collect(Collectors.toMap(PortfolioSnapshot::getAccountId, Function.identity()));

        Map<Long, AccountUserInfoRow> accountUsers = portfolioSnapshotRankingRepository.findAllAccountUserInfo().stream()
                .collect(Collectors.toMap(AccountUserInfoRow::accountId, Function.identity()));

        List<RankingResponse> sorted = latestSnapshots.entrySet().stream()
                .filter(entry -> baseSnapshots.containsKey(entry.getKey())) // 비교 대상 스냅샷 없으면 제외 (가입 N일 미만 등)
                .filter(entry -> accountUsers.containsKey(entry.getKey())) // 수익률 비공개 설정 계좌 제외
                .map(entry -> {
                    PortfolioSnapshot base = baseSnapshots.get(entry.getKey());
                    PortfolioSnapshot latest = entry.getValue();
                    AccountUserInfoRow user = accountUsers.get(entry.getKey());
                    BigDecimal returnRate = calculatePeriodReturn(base.getTotalAsset(), latest.getTotalAsset());
                    return new RankingResponse(0, user.userId(), user.nickname(), user.userLevel(), returnRate);
                })
                .sorted(Comparator.comparing(RankingResponse::returnRate).reversed())
                .toList();

        List<RankingResponse> ranked = assignRanks(sorted);

        rankingCacheService.refresh(period, ranked);
    }

    private BigDecimal calculatePeriodReturn(BigDecimal baseAsset, BigDecimal latestAsset) {
        if (baseAsset.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return latestAsset.subtract(baseAsset)
                .divide(baseAsset, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
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

    // 정렬된 리스트에 1등부터 순위를 다시 매겨서 새 리스트로 반환 (record는 불변이라 rank만 바꾼 새 객체 생성)
    private List<RankingResponse> assignRanks(List<RankingResponse> sorted) {
        return java.util.stream.IntStream.range(0, sorted.size())
                .mapToObj(i -> {
                    RankingResponse r = sorted.get(i);
                    return new RankingResponse(i + 1, r.userId(), r.nickname(), r.userLevel(), r.returnRate());
                })
                .toList();
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