package com.zerorisk.project.domain.chat.service;

import com.zerorisk.project.domain.chat.entity.ChatMessage;
import com.zerorisk.project.domain.chat.repository.ChatMessageRepository;
import com.zerorisk.project.domain.competition.entity.Competition;
import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import com.zerorisk.project.domain.competition.repository.CompetitionRepository;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRetentionService {

    private static final int STOCK_RETENTION_DAYS = 90;
    private static final int COMPETITION_RETENTION_DAYS_AFTER_END = 30;

    private final ChatMessageRepository chatMessageRepository;
    private final CompetitionRepository competitionRepository;

    // 종목 채팅: 90일 지난 메시지 소프트 삭제 (신고된 메시지는 제외)
    @Transactional
    public void cleanupStockMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(STOCK_RETENTION_DAYS);

        List<ChatMessage> targets = chatMessageRepository
                .findByChannelTypeAndIsDeletedFalseAndIsReportedFalseAndCreatedAtBefore(
                        ChatChannelType.STOCK, cutoff);

        targets.forEach(ChatMessage::softDelete);

        log.info("종목 채팅 보관 정책 정리 완료 - 삭제 대상: {}건", targets.size());
    }

    // 대회 채팅: 종료 후 30일 지난 대회의 메시지 전체 소프트 삭제 (신고된 메시지는 제외)
    @Transactional
    public void cleanupCompetitionMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(COMPETITION_RETENTION_DAYS_AFTER_END);

        List<Competition> endedCompetitions = competitionRepository
                .findByStatusAndEndAtBefore(CompetitionStatus.ENDED, cutoff);

        int totalDeleted = 0;

        for (Competition competition : endedCompetitions) {
            String channelId = String.valueOf(competition.getId());

            List<ChatMessage> targets = chatMessageRepository
                    .findByChannelTypeAndChannelIdAndIsDeletedFalseAndIsReportedFalse(
                            ChatChannelType.COMPETITION, channelId);

            targets.forEach(ChatMessage::softDelete);
            totalDeleted += targets.size();
        }

        log.info("대회 채팅 보관 정책 정리 완료 - 대상 대회: {}개, 삭제 메시지: {}건",
                endedCompetitions.size(), totalDeleted);
    }
}
