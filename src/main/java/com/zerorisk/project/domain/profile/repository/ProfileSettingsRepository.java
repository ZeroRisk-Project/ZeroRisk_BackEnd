package com.zerorisk.project.domain.profile.repository;

import com.zerorisk.project.domain.profile.entity.ProfileSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileSettingsRepository extends JpaRepository<ProfileSettings, Long> {
    Optional<ProfileSettings> findByUserId(Long userId);
}
