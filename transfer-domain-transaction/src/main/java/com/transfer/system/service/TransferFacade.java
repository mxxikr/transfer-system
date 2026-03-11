package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferFacade {
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    /**
     * 이체 오케스트레이션 (락 제어 및 트랜잭션 관리)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionEntity transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new TransferSystemException(ErrorCode.TRANSFER_SAME_ACCOUNT);
        }

        // 락 순서 고정 (데드락 방지)
        String first = fromAccountNumber.compareTo(toAccountNumber) <= 0 ? fromAccountNumber : toAccountNumber;
        String second = fromAccountNumber.compareTo(toAccountNumber) <= 0 ? toAccountNumber : fromAccountNumber;

        // 비관적 락 획득
        AccountEntity firstLock = accountRepository.findByAccountNumberLock(first)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        AccountEntity secondLock = accountRepository.findByAccountNumberLock(second)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        AccountEntity fromAccount = fromAccountNumber.equals(first) ? firstLock : secondLock;
        AccountEntity toAccount = fromAccountNumber.equals(first) ? secondLock : firstLock;

        // 핵심 비즈니스 로직 위임
        return transactionService.executeTransfer(fromAccount, toAccount, amount);
    }
}
