package com.zerorisk.project.domain.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "ACCOUNTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_seq")
    @SequenceGenerator(name = "accounts_seq", sequenceName = "ACCOUNTS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "BALANCE", nullable = false)
    private BigDecimal balance;

    @Column(name = "INITIAL_SEED_MONEY", nullable = false)
    private BigDecimal initialSeedMoney;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCOUNT_TYPE", nullable = false, length = 15)
    private AccountType accountType;

    @Column(name = "COMPETITION_ID")
    private Long competitionId;

    @ColumnDefault("1")
    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Account(Long userId, AccountType accountType, Long competitionId) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
        this.initialSeedMoney = BigDecimal.ZERO;
        this.accountType = accountType;
        this.competitionId = competitionId;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void zeroBalance() {
        this.balance = BigDecimal.ZERO;
    }

    public void addSeedMoney(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.initialSeedMoney = this.initialSeedMoney.add(amount);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}