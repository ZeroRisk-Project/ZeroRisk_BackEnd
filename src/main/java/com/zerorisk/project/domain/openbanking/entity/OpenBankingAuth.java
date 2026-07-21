package com.zerorisk.project.domain.openbanking.entity;

import com.zerorisk.project.domain.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "OPENBANKING_AUTHS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenBankingAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openbanking_auths_seq")
    @SequenceGenerator(name = "openbanking_auths_seq", sequenceName = "OPENBANKING_AUTHS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "BANK_NAME", nullable = false, length = 20)
    private String bankName;

    @Column(name = "ACCOUNT_NUM_MASKED", nullable = false, length = 30)
    private String accountNumMasked;

    @Column(name = "FINTECH_USE_NUM", length = 24)
    private String fintechUseNum;

    @Column(name = "VERIFIED_AT", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "TOTAL_RECEIVED_POINTS", nullable = false)
    private BigDecimal totalReceivedPoints;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @Builder
    private OpenBankingAuth(Long userId, String bankName, String accountNumMasked, String fintechUseNum) {
        this.userId = userId;
        this.bankName = bankName;
        this.accountNumMasked = accountNumMasked;
        this.fintechUseNum = fintechUseNum;
        this.verifiedAt = LocalDateTime.now();
        this.totalReceivedPoints = BigDecimal.ZERO;
    }

    public void addReceivedPoints(BigDecimal amount) {
        this.totalReceivedPoints = this.totalReceivedPoints.add(amount);
    }
}