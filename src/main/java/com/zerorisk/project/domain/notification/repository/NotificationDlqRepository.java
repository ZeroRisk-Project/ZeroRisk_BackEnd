package com.zerorisk.project.domain.notification.repository;

import com.zerorisk.project.domain.notification.entity.NotificationDlq;
import com.zerorisk.project.domain.notification.entity.NotificationDlqStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDlqRepository extends JpaRepository<NotificationDlq, Long> {

    Optional<NotificationDlq> findByIdAndStatus(Long id, NotificationDlqStatus status);

    Page<NotificationDlq> findByStatus(NotificationDlqStatus status, Pageable pageable);
}
