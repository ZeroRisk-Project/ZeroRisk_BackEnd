package com.zerorisk.project.domain.competition.controller;

import com.zerorisk.project.domain.competition.dto.MyPrizeHistoryResponse;
import com.zerorisk.project.domain.competition.service.CompetitionService;
import com.zerorisk.project.global.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/prizes")
@RequiredArgsConstructor
public class PrizeController {

    private final CompetitionService competitionService;

    @GetMapping("/me")
    public List<MyPrizeHistoryResponse> getMyPrizeHistory(@CurrentUserId Long userId) {
        return competitionService.getMyPrizeHistory(userId);
    }
}
