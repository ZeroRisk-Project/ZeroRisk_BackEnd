package com.zerorisk.project.domain.openbanking.repository;

import com.zerorisk.project.domain.openbanking.entity.MonthlyChargeSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyChargeSettingRepository extends JpaRepository<MonthlyChargeSetting, Long> {

    Optional<MonthlyChargeSetting> findByUserId(Long userId);

    List<MonthlyChargeSetting> findByChargeDayAndIsActiveTrue(Integer chargeDay);
}