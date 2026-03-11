package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface TransactionService {
    /**
     * 이체 기능
     */
    TransactionEntity transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount);

    /**
     * 계좌 입금
     */
    TransactionEntity deposit(String accountNumber, BigDecimal amount);

    /**
     * 계좌 출금
     */
    TransactionEntity withdraw(String accountNumber, BigDecimal amount);

    /**
     * 계좌 거래 내역 조회
     */
    Page<TransactionEntity> getTransactionHistory(String accountNumber, int page, int size);
}
