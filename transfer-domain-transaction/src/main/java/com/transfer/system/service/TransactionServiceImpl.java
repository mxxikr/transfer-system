package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import com.transfer.system.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferPolicy transferPolicy;

    /**
     * 핵심 이체 비즈니스 로직 (락 획득 후 실행)
     */
    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public TransactionEntity executeTransfer(AccountEntity fromAccount, AccountEntity toAccount, BigDecimal amount) {
        // 계좌 상태 및 무결성 검증
        validateTransferAbility(fromAccount, toAccount);

        // 수수료 계산
        BigDecimal fee = transferPolicy.calculateFee(amount);
        validateFee(fee);
        BigDecimal total = amount.add(fee);

        log.debug("[TransactionService] 이체 시작 - From: {}, To: {}, Amount: {}, Fee: {}, Total: {}", 
                fromAccount.getAccountNumber(), toAccount.getAccountNumber(), amount, fee, total);

        // 한도 확인
        validateTransferLimit(fromAccount.getAccountNumber(), amount);

        // 잔액 확인 및 차감
        if (fromAccount.getBalance().compareTo(total) < 0) {
            log.warn("[TransactionService] 잔액 부족 Account : {}, Total : {}, Balance : {}", 
                    fromAccount.getAccountNumber(), total, fromAccount.getBalance());
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 엔티티 상태 변경
        fromAccount.subtractBalance(total);
        toAccount.addBalance(amount);

        // 거래 내역 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .transactionType(TransactionType.TRANSFER)
                .amount(amount)
                .fee(fee)
                .build();

        return transactionRepository.save(transactionEntity);
    }

    private void validateTransferAbility(AccountEntity fromAccount, AccountEntity toAccount) {
        if (fromAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new TransferSystemException(ErrorCode.SENDER_ACCOUNT_INACTIVE);
        }
        if (toAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new TransferSystemException(ErrorCode.RECEIVER_ACCOUNT_INACTIVE);
        }
        if (fromAccount.getCurrencyType() == null || toAccount.getCurrencyType() == null || 
            !fromAccount.getCurrencyType().equals(toAccount.getCurrencyType())) {
            throw new TransferSystemException(ErrorCode.CURRENCY_TYPE_MISMATCH);
        }
    }

    private void validateFee(BigDecimal fee) {
        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.INVALID_FEE);
        }
    }

    private void validateTransferLimit(String accountNumber, BigDecimal amount) {
        LocalDateTime startTime = TimeUtils.startOfTodayKst();
        LocalDateTime endTime = TimeUtils.endOfTodayKst();

        BigDecimal todayUsed = transactionRepository.getSumTodayUsedAmount(accountNumber, TransactionType.TRANSFER, startTime, endTime);
        todayUsed = (todayUsed != null) ? todayUsed : BigDecimal.ZERO;

        transferPolicy.validateTransferAmount(amount, todayUsed);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionEntity deposit(String accountNumber, BigDecimal amount) {
        AccountEntity accountEntity = accountRepository.findByAccountNumberLock(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountEntity.addBalance(amount);
        accountRepository.save(accountEntity);

        TransactionEntity transactionEntity = TransactionEntity.builder()
            .toAccount(accountEntity)
            .transactionType(TransactionType.DEPOSIT)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .build();

        return transactionRepository.save(transactionEntity);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionEntity withdraw(String accountNumber, BigDecimal amount) {
        AccountEntity accountEntity = accountRepository.findByAccountNumberLock(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateWithdrawLimit(accountNumber, amount);

        if (accountEntity.getBalance().compareTo(amount) < 0) {
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        accountEntity.subtractBalance(amount);
        accountRepository.save(accountEntity);

        TransactionEntity transactionEntity = TransactionEntity.builder()
            .fromAccount(accountEntity)
            .transactionType(TransactionType.WITHDRAW)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .build();

        return transactionRepository.save(transactionEntity);
    }

    private void validateWithdrawLimit(String accountNumber, BigDecimal amount) {
        LocalDateTime startTime = TimeUtils.startOfTodayKst();
        LocalDateTime endTime   = TimeUtils.endOfTodayKst();

        BigDecimal todayUsed = transactionRepository.getSumTodayUsedAmount(accountNumber, TransactionType.WITHDRAW, startTime, endTime);
        todayUsed = (todayUsed != null) ? todayUsed : BigDecimal.ZERO;

        transferPolicy.validateWithdrawAmount(amount, todayUsed);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionEntity> getTransactionHistory(String accountNumber, int page, int size) {
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdTimeStamp").descending());
        return transactionRepository.findAllByAccountWithFetchJoin(account, pageable);
    }
}
