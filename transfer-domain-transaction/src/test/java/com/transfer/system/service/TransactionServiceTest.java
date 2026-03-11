package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferPolicy transferPolicy;

    private TransactionServiceImpl transactionService;
    private AccountEntity fromAccountEntity;
    private AccountEntity toAccountEntity;
    private TransactionEntity transactionEntity;

    private final String testFromAccountNumber = "00125080800001";
    private final String testToAccountNumber = "00125080800002";


    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(accountRepository, transactionRepository, transferPolicy);

        fromAccountEntity = AccountEntity.builder()
            .accountId(UUID.randomUUID())
            .accountNumber(testFromAccountNumber)
            .accountName("sender")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("200000"))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        toAccountEntity = AccountEntity.builder()
            .accountId(UUID.randomUUID())
            .accountNumber(testToAccountNumber)
            .accountName("receiver")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("100000"))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        transactionEntity = TransactionEntity.builder()
            .transactionId(UUID.randomUUID())
            .fromAccount(fromAccountEntity)
            .toAccount(toAccountEntity)
            .transactionType(TransactionType.TRANSFER)
            .amount(new BigDecimal("100000"))
            .fee(new BigDecimal("1000"))
            .build();
    }

    // ========================== 공통 메서드 =========================

    /**
     * 이체 시 예외 처리
     */
    private void expectTransferException(String from, String to, BigDecimal amount, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> transactionService.transfer(from, to, amount));
        assertEquals(expectedError, exception.getErrorCode());
    }

    /**
     * 오늘 사용 금액
     */
    private void todayUsed(String accountNumber, TransactionType type, BigDecimal used) {
        when(transactionRepository.getSumTodayUsedAmount(
                eq(accountNumber), eq(type),
                any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(used);
    }

    // ========================= 이체 테스트 =========================
    @Nested
    class TransferTest {

        /**
         * 이체 성공
         */
        @Test
        void transfer_success() {
            BigDecimal amount = new BigDecimal("100000");
            BigDecimal fee = new BigDecimal("1000");

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(fee);

            todayUsed(testFromAccountNumber, TransactionType.TRANSFER, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateTransferAmount(any(BigDecimal.class), any(BigDecimal.class));
            when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(transactionEntity);

            transactionService.transfer(testFromAccountNumber, testToAccountNumber, amount);

            ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
            verify(transactionRepository).save(transactionCaptor.capture());

            TransactionEntity savedTransaction = transactionCaptor.getValue();

            assertEquals(fromAccountEntity, savedTransaction.getFromAccount());
            assertEquals(toAccountEntity, savedTransaction.getToAccount());
            assertEquals(amount, savedTransaction.getAmount());
            assertEquals(fee, savedTransaction.getFee());
        }

        /**
         * 이체 시 동일 계좌 간 이체
         */
        @Test
        void transfer_sameAccount() {
            expectTransferException(testFromAccountNumber, testFromAccountNumber, new BigDecimal("100000"), ErrorCode.TRANSFER_SAME_ACCOUNT);
        }

        /**
         * 이체 시 출금 계좌 없음
         */
        @Test
        void transfer_fromAccountNotFound() {
            // 정렬 순서에 따라 락을 검. "00125080800001" vs "00125080800002" -> from이 먼저
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.empty());

            expectTransferException(testFromAccountNumber, testToAccountNumber, new BigDecimal("100000"), ErrorCode.ACCOUNT_NOT_FOUND);
        }

        /**
         * 이체 시 입금 계좌 없음
         */
        @Test
        void transfer_toAccountNotFound() {
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.empty());

            expectTransferException(testFromAccountNumber, testToAccountNumber, new BigDecimal("100000"), ErrorCode.ACCOUNT_NOT_FOUND);
        }

        /**
         * 이체 시 송신 계좌 비활성
         */
        @Test
        void transfer_fromInactiveAccount() {
            fromAccountEntity.setAccountStatus(AccountStatus.INACTIVE);

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));

            expectTransferException(testFromAccountNumber, testToAccountNumber, new BigDecimal("100000"), ErrorCode.SENDER_ACCOUNT_INACTIVE);
        }

        /**
         * 이체 시 수신 계좌 비활성
         */
        @Test
        void transfer_toInactiveAccount() {
            toAccountEntity.setAccountStatus(AccountStatus.INACTIVE);

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));

            expectTransferException(testFromAccountNumber, testToAccountNumber, new BigDecimal("100000"), ErrorCode.RECEIVER_ACCOUNT_INACTIVE);
        }

        /**
         * 이체 시 통화 불일치
         */
        @Test
        void transfer_currencyMismatch() {
            toAccountEntity.setCurrencyType(CurrencyType.USD);

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));

            expectTransferException(testFromAccountNumber, testToAccountNumber, new BigDecimal("100000"), ErrorCode.CURRENCY_TYPE_MISMATCH);
        }

        /**
         * 이체 시 잔액 부족
         */
        @Test
        void transfer_insufficientBalance() {
            fromAccountEntity.setBalance(new BigDecimal("50000"));
            BigDecimal amount = new BigDecimal("100000");

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("1000"));

            todayUsed(testFromAccountNumber, TransactionType.TRANSFER, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateTransferAmount(any(BigDecimal.class), any(BigDecimal.class));

            expectTransferException(testFromAccountNumber, testToAccountNumber, amount, ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    // ========================= 입금/출금 테스트 =========================
    @Nested
    class DepositWithdrawTest {

        @Test
        void deposit_success() {
            BigDecimal amount = new BigDecimal("50000");
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            transactionService.deposit(testFromAccountNumber, amount);

            assertEquals(0, fromAccountEntity.getBalance().compareTo(new BigDecimal("250000")));
            verify(transactionRepository).save(argThat(t -> t.getTransactionType() == TransactionType.DEPOSIT));
        }

        @Test
        void withdraw_success() {
            BigDecimal amount = new BigDecimal("50000");
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            todayUsed(testFromAccountNumber, TransactionType.WITHDRAW, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateWithdrawAmount(any(), any());
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            transactionService.withdraw(testFromAccountNumber, amount);

            assertEquals(0, fromAccountEntity.getBalance().compareTo(new BigDecimal("150000")));
            verify(transactionRepository).save(argThat(t -> t.getTransactionType() == TransactionType.WITHDRAW));
        }

        @Test
        void withdraw_insufficientBalance() {
            BigDecimal amount = new BigDecimal("300000");
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            todayUsed(testFromAccountNumber, TransactionType.WITHDRAW, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateWithdrawAmount(any(), any());

            assertThrows(TransferSystemException.class, () -> transactionService.withdraw(testFromAccountNumber, amount));
        }
    }

    // ========================= 거래 내역 조회 테스트 =========================
    @Nested
    class GetTransactionHistoryTest {

        /**
         * 거래 내역 조회 성공
         */
        @Test
        void getTransactionHistory_success() {
            List<TransactionEntity> transactions = List.of(transactionEntity);
            Page<TransactionEntity> transactionPage = new PageImpl<>(transactions);

            when(accountRepository.findByAccountNumber(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(transactionRepository.findAllByAccount(eq(fromAccountEntity), any(Pageable.class))).thenReturn(transactionPage);

            Page<TransactionEntity> result = transactionService.getTransactionHistory(testFromAccountNumber, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(transactionRepository).findAllByAccount(eq(fromAccountEntity), any(Pageable.class));
        }

        /**
         * 거래 내역 조회 시 계좌가 존재하지 않는 경우
         */
        @Test
        void getTransactionHistory_accountNotFound() {
            when(accountRepository.findByAccountNumber("nonexistent")).thenReturn(Optional.empty());

            assertThrows(TransferSystemException.class, () -> transactionService.getTransactionHistory("nonexistent", 0, 10));
        }
    }
}
