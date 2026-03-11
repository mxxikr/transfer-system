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
import com.transfer.system.utils.MoneyUtils;
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
     * 이체 기능
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionEntity transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (fromAccountNumber.equals(toAccountNumber)) { // 같은 계좌로는 이체할 수 없음
            throw new TransferSystemException(ErrorCode.TRANSFER_SAME_ACCOUNT);
        }

        log.debug("[TransactionService] From: {}, To: {}, Amount: {}", fromAccountNumber, toAccountNumber, amount);

        // 락 순서 고정
        String firstAccountNumber = fromAccountNumber.compareTo(toAccountNumber) <= 0 ? fromAccountNumber : toAccountNumber;
        String secondAccountNumber = fromAccountNumber.compareTo(toAccountNumber) <= 0 ? toAccountNumber : fromAccountNumber;

        // 비관적 락
        AccountEntity firstLock = accountRepository.findByAccountNumberLock(firstAccountNumber).orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        AccountEntity secondLock = accountRepository.findByAccountNumberLock(secondAccountNumber).orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 송신 계좌 기준 매핑
        AccountEntity fromAccount = firstAccountNumber.equals(fromAccountNumber) ? firstLock : secondLock;
        AccountEntity toAccount   = getAccountEntity(fromAccount, firstLock, secondLock);

        // 이체 수수료 유효성 검사
        BigDecimal fee = transferPolicy.calculateFee(amount); // 이체 수수료 계산

        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.INVALID_FEE);
        }

        BigDecimal total = amount.add(fee); // 총 이쳬 금액


        log.debug("[TransactionService] 수수료 계산 결과 Amount : {}, Fee : {}, Total : {}", amount, fee, total);

        // 이체 한도 확인
        LocalDateTime startTime = TimeUtils.startOfTodayKst();
        LocalDateTime endTime = TimeUtils.endOfTodayKst();

        BigDecimal todayUsed = transactionRepository.getSumTodayUsedAmount(fromAccountNumber, TransactionType.TRANSFER, startTime, endTime);
        todayUsed = todayUsed != null ? todayUsed : BigDecimal.ZERO;

        transferPolicy.validateTransferAmount(amount, todayUsed);

        // 잔액 확인
        if (fromAccount.getBalance().compareTo(total) < 0) { // 출금 계좌의 잔액이 이체 금액보다 많아야 함
            log.warn("[TransactionService] 잔액 부족 Account : {}, 이체 금액 : {}, 총 잔액 : {}", fromAccountNumber, total, fromAccount.getBalance());
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 계좌 잔액 업데이트
        fromAccount.subtractBalance(total);
        toAccount.addBalance(amount);

        // 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .transactionType(TransactionType.TRANSFER)
                .amount(amount)
                .fee(fee)
                .build();

        TransactionEntity savedTransactionEntity = transactionRepository.save(transactionEntity);
        log.debug("[TranscationService] 이체 완료 거래ID : {}", savedTransactionEntity.getTransactionId());

        return savedTransactionEntity;
    }

    private static AccountEntity getAccountEntity(AccountEntity fromAccount, AccountEntity firstLock, AccountEntity secondLock) {
        AccountEntity toAccount = fromAccount == firstLock ? secondLock : firstLock;

        // 수신 계좌 상태 확인
        if (toAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new TransferSystemException(ErrorCode.RECEIVER_ACCOUNT_INACTIVE);
        }

        // 송신 계좌 상태 확인
        if (fromAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new TransferSystemException(ErrorCode.SENDER_ACCOUNT_INACTIVE);
        }

        // 통화 종류 일치 확인
        if (fromAccount.getCurrencyType() == null || toAccount.getCurrencyType() == null || !fromAccount.getCurrencyType().equals(toAccount.getCurrencyType())) {
            throw new TransferSystemException(ErrorCode.CURRENCY_TYPE_MISMATCH);
        }
        return toAccount;
    }

    /**
     * 계좌 입금
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionEntity deposit(String accountNumber, BigDecimal amount) {
        AccountEntity accountEntity = accountRepository.findByAccountNumberLock(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountEntity.addBalance(amount);
        accountRepository.save(accountEntity);

        // 입금 거래 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .fromAccount(null)
            .toAccount(accountEntity)
            .transactionType(TransactionType.DEPOSIT)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .build();

        TransactionEntity savedTransactionEntity = transactionRepository.save(transactionEntity);
        log.debug("[TransactionService] 입금 완료 transactionId: {}, accountNumber: {}", savedTransactionEntity.getTransactionId(), accountNumber);

        return savedTransactionEntity;
    }

    /**
     * 계좌 출금
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionEntity withdraw(String accountNumber, BigDecimal amount) {
        AccountEntity accountEntity = accountRepository.findByAccountNumberLock(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        LocalDateTime startTime = TimeUtils.startOfTodayKst();
        LocalDateTime endTime   = TimeUtils.endOfTodayKst();

        BigDecimal todayUsed = transactionRepository.getSumTodayUsedAmount(accountNumber, TransactionType.WITHDRAW, startTime, endTime);
        todayUsed = todayUsed != null ? todayUsed : BigDecimal.ZERO;

        transferPolicy.validateWithdrawAmount(amount, todayUsed);

        if (accountEntity.getBalance().compareTo(amount) < 0) {
            log.warn("[TransactionService] 잔액 부족 accountNumber: {}, request: {}, balance: {}", accountNumber, amount, accountEntity.getBalance());
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        accountEntity.subtractBalance(amount);
        accountRepository.save(accountEntity);

        // 출금 거래 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .fromAccount(accountEntity)
            .toAccount(null)
            .transactionType(TransactionType.WITHDRAW)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .build();

        TransactionEntity savedTransactionEntity = transactionRepository.save(transactionEntity);
        log.debug("[TransactionService] 출금 완료 transactionId: {}, accountNumber: {}", savedTransactionEntity.getTransactionId(), accountNumber);

        return savedTransactionEntity;
    }

    /**
     * 계좌 거래 내역 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionEntity> getTransactionHistory(String accountNumber, int page, int size) {
        // 계좌 존재 여부 확인
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 페이징은 컨트롤러 또는 API 레이어에서 처리하도록 하고 여기서는 직접 Pageable 생성
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdTimeStamp").descending());

        Page<TransactionEntity> transactions = transactionRepository.findAllByAccount(account, pageable);

        log.info("[TransactionService] 거래 내역 조회 완료: 총 {}건, 현재 페이지 {}건", transactions.getTotalElements(), transactions.getNumberOfElements());

        return transactions;
    }
}