package com.zerorisk.project.domain.notification.repository;

import com.zerorisk.project.domain.notification.entity.AlertSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertSettingsRepository extends JpaRepository<AlertSettings, Long> {

    Optional<AlertSettings> findByUserId(Long userId);
}