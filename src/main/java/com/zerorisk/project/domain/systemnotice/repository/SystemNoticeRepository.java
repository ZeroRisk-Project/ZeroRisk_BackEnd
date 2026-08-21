package com.zerorisk.project.domain.systemnotice.repository;

import com.zerorisk.project.domain.systemnotice.entity.SystemNotice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemNoticeRepository extends JpaRepository<SystemNotice, Long> {
    List<SystemNotice> findAllByIsActiveTrueOrderByCreatedAtDesc();
    List<SystemNotice> findAllByOrderByCreatedAtDesc();
}
