package com.zerorisk.project.domain.announcement.entity;

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
@Table(name = "ANNOUNCEMENTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "announcements_seq")
    @SequenceGenerator(name = "announcements_seq", sequenceName = "ANNOUNCEMENTS_SEQ", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TAG", nullable = false, length = 20)
    private AnnouncementTag tag;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "IS_IMPORTANT", nullable = false)
    private Boolean isImportant;

    @Column(name = "CREATED_BY", nullable = false)
    private Long createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Announcement(AnnouncementTag tag, String title, String content, Boolean isImportant, Long createdBy) {
        this.tag = tag;
        this.title = title;
        this.content = content;
        this.isImportant = isImportant != null ? isImportant : false;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public void update(AnnouncementTag tag, String title, String content, Boolean isImportant) {
        this.tag = tag;
        this.title = title;
        this.content = content;
        this.isImportant = isImportant;
    }
}
