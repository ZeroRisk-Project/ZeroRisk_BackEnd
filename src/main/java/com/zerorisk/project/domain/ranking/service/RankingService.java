package com.zerorisk.project.domain.ranking.service;

import com.zerorisk.project.domain.ranking.cache.RankingCacheService;
import com.zerorisk.project.domain.ranking.dto.AccountReturnRateRow;
import com.zerorisk.project.domain.ranking.dto.RankingResponse;
import com.zerorisk.project.domain.ranking.repository.PortfolioSnapshotRankingRepository;
import com.zerorisk.project.global.exception.MyRankingNotFoundException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final PortfolioSnapshotRankingRepository portfolioSnapshotRankingRepository;
    private final RankingCacheService rankingCacheService;

    // Scheduler 전용 - 무거운 연산이라 요청 시점마다 호출하면 안 됨
    public void refreshRanking() {
        List<AccountReturnRateRow> rows = portfolioSnapshotRankingRepository.findAllReturnRates();

        List<RankingResponse> sorted = rows.stream()
                .map(row -> RankingResponse.of(0, row))
                .sorted(Comparator.comparing(RankingResponse::returnRate).reversed())
                .toList();

        List<RankingResponse> ranked = assignRanks(sorted);

        rankingCacheService.refresh(ranked);
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

    public List<RankingResponse> getRankings(int page, int size) {
        return rankingCacheService.getTopRankings(page, size);
    }

    public RankingResponse getMyRanking(Long userId) {
        RankingResponse ranking = rankingCacheService.getMyRanking(userId);

        if (ranking == null) {
            throw new MyRankingNotFoundException();
        }

        return ranking;
    }
}