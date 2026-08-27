package com.zerorisk.project.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "USERS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EMAIL", nullable = false, length = 100)
    private String email;

    @Column(name = "NICKNAME", nullable = false, length = 12)
    private String nickname;

    @Column(name = "PASSWORD", length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_ROLE", nullable = false, length = 10)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "OAUTH_PROVIDER", length = 20)
    private OAuthProvider oauthProvider;

    @Lob
    @Column(name = "PROFILE_IMAGE_URL")
    private String profileImageUrl;

    @Column(name = "ACTIVITY_SCORE", nullable = false)
    private Integer activityScore;

    @Column(name = "USER_LEVEL", nullable = false)
    private Integer userLevel;

    @Column(name = "SUSPENDED_UNTIL")
    private LocalDateTime suspendedUntil;

    @Lob
    @Column(name = "SUSPENSION_REASON")
    private String suspensionReason;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "OAUTH_PROVIDER_ID", length = 50)
    private String oauthProviderId;

    @Column(name = "HAS_CLAIMED_PRACTICE_CREDIT", nullable = false)
    private Boolean hasClaimedPracticeCredit;

    @Builder
    private User(String email, String nickname, String password, OAuthProvider oauthProvider, String oauthProviderId) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.userRole = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.oauthProvider = oauthProvider;
        this.oauthProviderId = oauthProviderId;
        this.activityScore = 0;
        this.userLevel = 1;
        this.createdAt = LocalDateTime.now();
        this.hasClaimedPracticeCredit = false;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void withdraw() {
        this.status = UserStatus.QUIT;
        this.nickname = "탈퇴" + this.id; // NICKNAME 컬럼이 길이 12 제한이라 짧게 (id는 PK라 충돌 걱정 없음)
    }

    public void suspend(LocalDateTime until, String reason) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedUntil = until;
        this.suspensionReason = reason;
    }

    public void unsuspend() {
        this.status = UserStatus.ACTIVE;
        this.suspendedUntil = null;
        this.suspensionReason = null;
    }

    public void addActivityScore(int score) {
        this.activityScore += score;
    }

    public void claimPracticeCredit() {
        this.hasClaimedPracticeCredit = true;
    }

    public static User createOAuthUser(String email, String nickname, OAuthProvider provider, String providerId) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .password(null)
                .oauthProvider(provider)
                .oauthProviderId(providerId)
                .build();
    }
}