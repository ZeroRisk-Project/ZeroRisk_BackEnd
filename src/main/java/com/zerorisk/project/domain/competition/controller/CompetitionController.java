package com.zerorisk.project.domain.competition.controller;

import com.zerorisk.project.domain.competition.dto.CompetitionArchiveResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionDetailResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionRankingResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionSummaryResponse;
import com.zerorisk.project.domain.competition.dto.JoinCompetitionResponse;
import com.zerorisk.project.domain.competition.dto.JoinStatusResponse;
import com.zerorisk.project.domain.competition.dto.MyJoinedCompetitionsResponse;
import com.zerorisk.project.domain.competition.service.CompetitionService;
import com.zerorisk.project.global.security.CurrentUserId;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    public Page<CompetitionSummaryResponse> getCompetitions(Pageable pageable) {
        return competitionService.getCompetitions(pageable);
    }

    @GetMapping("/my")
    public MyJoinedCompetitionsResponse getMyJoinedCompetitions(@CurrentUserId Long userId) {
        return competitionService.getMyJoinedCompetitions(userId);
    }

    @GetMapping("/{competitionId}")
    public CompetitionDetailResponse getCompetitionDetail(@PathVariable Long competitionId) {
        return competitionService.getCompetitionDetail(competitionId);
    }

    @PostMapping("/{competitionId}/join")
    public JoinCompetitionResponse join(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return competitionService.joinCompetition(competitionId, userId);
    }

    @GetMapping("/{competitionId}/join-status")
    public JoinStatusResponse getJoinStatus(
            @PathVariable Long competitionId,
            @CurrentUserId Long userId) {
        return new JoinStatusResponse(competitionService.isJoined(competitionId, userId));
    }

    @GetMapping("/{competitionId}/rankings")
    public List<CompetitionRankingResponse> getRankings(@PathVariable Long competitionId) {
        return competitionService.getRankings(competitionId);
    }

    @GetMapping("/{competitionId}/archive")
    public CompetitionArchiveResponse getArchive(@PathVariable Long competitionId) {
        return competitionService.getArchive(competitionId);
    }
}