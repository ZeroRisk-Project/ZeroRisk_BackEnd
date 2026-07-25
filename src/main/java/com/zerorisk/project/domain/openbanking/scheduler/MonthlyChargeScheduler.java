package com.zerorisk.project.domain.openbanking.scheduler;

import com.zerorisk.project.domain.openbanking.entity.MonthlyChargeSetting;
import com.zerorisk.project.domain.openbanking.exception.OpenBankingException;
import com.zerorisk.project.domain.openbanking.repository.MonthlyChargeSettingRepository;
import com.zerorisk.project.domain.openbanking.service.OpenBankingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyChargeScheduler {

    private final MonthlyChargeSettingRepository monthlyChargeSettingRepository;
    private final OpenBankingService openBankingService;

    @Scheduled(cron = "0 0 0 * * *")
    public void executeMonthlyCharges() {
        int today = LocalDate.now().getDayOfMonth();
        List<MonthlyChargeSetting> targets = monthlyChargeSettingRepository.findByChargeDayAndIsActiveTrue(today);

        log.info("월 충전 배치 시작 - 대상 유저 수: {}", targets.size());

        for (MonthlyChargeSetting setting : targets) {
            try {
                openBankingService.chargeSeedMoney(setting.getUserId(), setting.getChargeAmount());
                setting.markCharged(LocalDateTime.now());
                log.info("월 충전 성공 - userId: {}", setting.getUserId());
            } catch (OpenBankingException e) {
                log.warn("월 충전 실패 - userId: {}, reason: {}", setting.getUserId(), e.getMessage());
            }
        }

        log.info("월 충전 배치 종료");
    }
}