package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import java.util.Optional;

public interface AccountRepositoryCustom {
    /**
     * 계좌 번호로 계좌를 비관적 락(PESSIMISTIC_WRITE)과 함께 조회
     */
    Optional<AccountEntity> findByAccountNumberLock(String accountNumber);
}
