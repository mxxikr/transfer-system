package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByAccountNumber(String accountNumber); // 계좌 번호로 계좌 조회
    boolean existsByAccountNumber(String accountNumber); // 계좌 번호로 계좌 존재 여부 확인

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ae
        FROM AccountEntity ae
        WHERE ae.accountNumber = :accountNumber
    """)
    Optional<AccountEntity> findByAccountNumberLock(@Param("accountNumber") String accountNumber);
}