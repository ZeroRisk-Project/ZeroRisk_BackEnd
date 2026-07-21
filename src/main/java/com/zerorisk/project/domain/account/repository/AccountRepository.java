package com.zerorisk.project.domain.account.repository;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.userId = :userId AND a.accountType = 'BASIC'")
    Optional<Account> findBasicAccountByUserIdForUpdate(java.lang.Long userId);

    Optional<Account> findByUserIdAndAccountType(Long userId, AccountType accountType);
}