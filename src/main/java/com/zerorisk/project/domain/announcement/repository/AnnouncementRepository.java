package com.zerorisk.project.domain.announcement.repository;

import com.zerorisk.project.domain.announcement.entity.Announcement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOrderByIsImportantDescCreatedAtDesc();
}
