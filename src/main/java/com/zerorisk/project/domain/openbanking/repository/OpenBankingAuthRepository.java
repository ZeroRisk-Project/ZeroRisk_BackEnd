package com.zerorisk.project.domain.openbanking.repository;

import com.zerorisk.project.domain.openbanking.entity.OpenBankingAuth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenBankingAuthRepository extends JpaRepository<OpenBankingAuth, Long> {
    Optional<OpenBankingAuth> findByUserId(Long userId);
}