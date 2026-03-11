package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionRepositoryCustom {
    Page<TransactionEntity> findAllByAccountWithFetchJoin(AccountEntity account, Pageable pageable);

    BigDecimal getSumTodayUsedAmount(String accountNumber, TransactionType type, LocalDateTime startTime, LocalDateTime endTime);

    boolean existsByFromOrTo(AccountEntity accountEntity);
}
