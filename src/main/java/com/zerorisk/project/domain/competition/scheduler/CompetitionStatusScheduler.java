package com.zerorisk.project.domain.competition.scheduler;

import com.zerorisk.project.domain.competition.entity.Competition;
import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import com.zerorisk.project.domain.competition.repository.CompetitionRepository;
import com.zerorisk.project.domain.competition.service.CompetitionService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionStatusScheduler {

    private final CompetitionRepository competitionRepository;
    private final CompetitionService competitionService;

    @Scheduled(cron = "0 */10 * * * *")
    // @Scheduled(cron = "0 * * * * *") // 테스트용: 매분
    @Transactional
    public void transitionCompetitionStatus() {
        startScheduledCompetitions();
        startCalculatingCompetitions();
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void processCalculatingCompetitions() {
        List<Competition> targets = competitionRepository.findByStatus(CompetitionStatus.CALCULATING);

        for (Competition competition : targets) {
            try {
                competitionService.distributePrizes(competition.getId());
                log.info("대회 종료 및 상금 지급 완료 - competitionId: {}", competition.getId());
            } catch (Exception e) {
                log.warn("대회 종료 처리 실패 - competitionId: {}, reason: {}", competition.getId(), e.getMessage());
            }
        }
    }

    private void startScheduledCompetitions() {
        LocalDateTime now = LocalDateTime.now();
        List<Competition> targets = competitionRepository
                .findByStatusAndStartAtBefore(CompetitionStatus.SCHEDULED, now);

        for (Competition competition : targets) {
            try {
                competitionService.startCompetition(competition.getId());
                log.info("대회 시작 처리 완료 - competitionId: {}", competition.getId());
            } catch (Exception e) {
                log.warn("대회 시작 처리 실패 - competitionId: {}, reason: {}", competition.getId(), e.getMessage());
            }
        }
    }

    private void startCalculatingCompetitions() {
        LocalDateTime now = LocalDateTime.now();
        List<Competition> targets = competitionRepository
                .findByStatusAndEndAtBefore(CompetitionStatus.ONGOING, now);

        for (Competition competition : targets) {
            try {
                competition.startCalculating();
                log.info("대회 결과 집계 시작 - competitionId: {}", competition.getId());
            } catch (Exception e) {
                log.warn("대회 결과 집계 시작 실패 - competitionId: {}, reason: {}", competition.getId(), e.getMessage());
            }
        }
    }
}