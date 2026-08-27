package com.zerorisk.project.domain.ranking.controller;

import com.zerorisk.project.domain.ranking.dto.RankingPeriod;
import com.zerorisk.project.domain.ranking.dto.RankingResponse;
import com.zerorisk.project.domain.ranking.service.RankingService;
import com.zerorisk.project.global.security.CurrentUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public List<RankingResponse> getRankings(
            @RequestParam(defaultValue = "ALL") RankingPeriod period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return rankingService.getRankings(period, page, size);
    }

    @GetMapping("/me")
    public RankingResponse getMyRanking(
            @RequestParam(defaultValue = "ALL") RankingPeriod period,
            @CurrentUserId Long userId
    ) {
        return rankingService.getMyRanking(period, userId);
    }
}
