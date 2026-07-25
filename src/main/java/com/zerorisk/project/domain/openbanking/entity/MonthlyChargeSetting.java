package com.zerorisk.project.domain.openbanking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MONTHLY_CHARGE_SETTINGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyChargeSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "monthly_charge_settings_seq")
    @SequenceGenerator(name = "monthly_charge_settings_seq", sequenceName = "MONTHLY_CHARGE_SETTINGS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "CHARGE_DAY", nullable = false)
    private Integer chargeDay;

    @Column(name = "CHARGE_AMOUNT", nullable = false)
    private BigDecimal chargeAmount;

    @Column(name = "LAST_CHARGED_AT")
    private LocalDateTime lastChargedAt;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;

    @Builder
    private MonthlyChargeSetting(Long userId, Integer chargeDay, BigDecimal chargeAmount) {
        this.userId = userId;
        this.chargeDay = chargeDay;
        this.chargeAmount = chargeAmount;
        this.isActive = true;
    }

    public void updateSetting(Integer chargeDay, BigDecimal chargeAmount) {
        this.chargeDay = chargeDay;
        this.chargeAmount = chargeAmount;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void markCharged(LocalDateTime chargedAt) {
        this.lastChargedAt = chargedAt;
    }
}