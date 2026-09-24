package com.zerorisk.project.domain.competition.scheduler;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.competition.entity.Competition;
import com.zerorisk.project.domain.competition.entity.CompetitionParticipant;
import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantRepository;
import com.zerorisk.project.domain.competition.repository.CompetitionRepository;
import com.zerorisk.project.domain.competition.service.CompetitionService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// app.scheduling.enabled=false(벤치마크 프로필 등)일 때만 비활성화 - 값이 없으면(matchIfMissing)
// 기존과 동일하게 항상 켜짐. 실서비스 등 다른 프로필엔 영향 없음.
@Slf4j
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CompetitionStatusScheduler {

    private final CompetitionRepository competitionRepository;
    private final CompetitionParticipantRepository competitionParticipantRepository;
    private final AccountRepository accountRepository;
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

                // 재평가가 실제로 끝날 때까지, 그 사이의 시세 변동을 이용한 거래를 막기 위해
                // 참가자 전원의 계좌를 비활성화한다. (종료 시각과 실제 재평가 실행 시각 사이의
                // 스케줄러 주기만큼의 시차 동안 거래가 계속 가능했던 문제 수정)
                List<CompetitionParticipant> participants = competitionParticipantRepository
                        .findByCompetitionId(competition.getId());
                for (CompetitionParticipant participant : participants) {
                    accountRepository.findById(participant.getAccountId())
                            .ifPresent(Account::deactivate);
                }

                log.info("대회 결과 집계 시작 - competitionId: {}", competition.getId());
            } catch (Exception e) {
                log.warn("대회 결과 집계 시작 실패 - competitionId: {}, reason: {}", competition.getId(), e.getMessage());
            }
        }
    }
}