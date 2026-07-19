package com.zerorisk.project.domain.notification.entity;

import com.zerorisk.project.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ALERT_SETTINGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_settings_seq")
    @SequenceGenerator(name = "alert_settings_seq", sequenceName = "ALERT_SETTINGS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private User user;

    @Column(name = "ORDER_FILLED", nullable = false)
    private Boolean orderFilled;

    @Column(name = "COMMENT_ADDED", nullable = false)
    private Boolean commentAdded;

    @Column(name = "COMPETITION", nullable = false)
    private Boolean competition;

    @Column(name = "PRICE_ALERT", nullable = false)
    private Boolean priceAlert;

    @Column(name = "INQUIRY_ANSWERED", nullable = false)
    private Boolean inquiryAnswered;

    @Builder
    private AlertSettings(User user) {
        this.user = user;
        this.orderFilled = true;
        this.commentAdded = true;
        this.competition = true;
        this.priceAlert = true;
        this.inquiryAnswered = true;
    }

    public void update(
            boolean orderFilled,
            boolean commentAdded,
            boolean competition,
            boolean priceAlert,
            boolean inquiryAnswered) {
        this.orderFilled = orderFilled;
        this.commentAdded = commentAdded;
        this.competition = competition;
        this.priceAlert = priceAlert;
        this.inquiryAnswered = inquiryAnswered;
    }

    public boolean isEnabled(NotificationType type) {
        return switch (type) {
            case ORDER_FILLED -> orderFilled;
            case COMMENT_ADDED -> commentAdded;
            case COMPETITION -> competition;
            case PRICE_ALERT -> priceAlert;
            case INQUIRY_ANSWERED -> inquiryAnswered;
        };
    }
}