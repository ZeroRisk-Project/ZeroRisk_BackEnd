package com.zerorisk.project.domain.profile.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PROFILE_SETTINGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_settings_seq")
    @SequenceGenerator(name = "profile_settings_seq", sequenceName = "PROFILE_SETTINGS_SEQ", allocationSize = 50)
    private Long id;

    @Column(name = "USER_ID", nullable = false, unique = true)
    private Long userId;

    @Column(name = "SHOW_RETURN_RATE", nullable = false)
    private Boolean showReturnRate;

    @Column(name = "SHOW_PORTFOLIO", nullable = false)
    private Boolean showPortfolio;

    @Column(name = "SHOW_TRADES", nullable = false)
    private Boolean showTrades;

    @Column(name = "SHOW_STATS", nullable = false)
    private Boolean showStats;

    @Column(name = "SHOW_COMPETITIONS", nullable = false)
    private Boolean showCompetitions;

    @Builder
    private ProfileSettings(Long userId) {
        this.userId = userId;
        this.showReturnRate = true;
        this.showPortfolio = false;
        this.showTrades = false;
        this.showStats = true;
        this.showCompetitions = true;
    }

    public void update(Boolean showReturnRate, Boolean showPortfolio, Boolean showTrades,
            Boolean showStats, Boolean showCompetitions) {
        this.showReturnRate = showReturnRate;
        this.showPortfolio = showPortfolio;
        this.showTrades = showTrades;
        this.showStats = showStats;
        this.showCompetitions = showCompetitions;
    }
}
