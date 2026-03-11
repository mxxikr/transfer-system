package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import com.transfer.system.utils.MoneyUtils;
import com.transfer.system.utils.TimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferPolicy transferPolicy;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountNumberGeneratorService accountNumberGeneratorService;

    private AccountServiceImpl accountService;

    private AccountCreateRequestDTO accountCreateRequestDTO;
    private AccountEntity accountEntity;
    private TransactionEntity mockTransaction;
    private final UUID testAccountId = UUID.randomUUID();
    private final String testAccountNumber = "00125080800001";

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, transferPolicy, transactionRepository, accountNumberGeneratorService);

        accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountName("mxxikr")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .build();

        accountEntity = AccountEntity.builder()
                .accountId(testAccountId)
                .accountNumber(testAccountNumber)
                .accountName("mxxikr")
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("100000"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        mockTransaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .fromAccount(accountEntity)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .fee(BigDecimal.ZERO)
                .build();

        lenient().when(accountRepository.save(any(AccountEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ========================== 공통 메서드 =========================

    /**
     * 금액 정규화
     */
    private static BigDecimal MoneyNormalize(BigDecimal v) {
        return MoneyUtils.normalize(v);
    }

    /**
     * 계좌 생성 시 예외 처리
     */
    private void expectCreateAccountException(AccountCreateRequestDTO dto, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> accountService.createAccount(dto));
        assertEquals(expectedError, exception.getErrorCode());
    }

    /**
     * 입금 시 예외 처리
     */
    private void expectDepositException(String accountNumber, BigDecimal amount, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> accountService.deposit(accountNumber, amount));
        assertEquals(expectedError, exception.getErrorCode());
    }

    /**
     * 출금 시 예외 처리
     */
    private void expectWithdrawException(String accountNumber, BigDecimal amount, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> accountService.withdraw(accountNumber, amount));
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

    // ========================= 계좌 생성 테스트 =========================
    @Nested
    class CreateAccountTest {

        /**
         * 계좌 생성 성공
         */
        @Test
        void createAccount_success() {
            when(accountNumberGeneratorService.generateAccountNumber()).thenReturn(testAccountNumber);
            when(accountRepository.existsByAccountNumber(testAccountNumber)).thenReturn(false);

            accountService.createAccount(accountCreateRequestDTO);

            ArgumentCaptor<AccountEntity> accountCaptor = ArgumentCaptor.forClass(AccountEntity.class);
            verify(accountRepository).save(accountCaptor.capture());

            AccountEntity savedAccount = accountCaptor.getValue();

            assertEquals(testAccountNumber, savedAccount.getAccountNumber());
            assertEquals(accountCreateRequestDTO.getAccountName(), savedAccount.getAccountName());
            assertEquals(accountCreateRequestDTO.getAccountType(), savedAccount.getAccountType());
            assertEquals(accountCreateRequestDTO.getCurrencyType(), savedAccount.getCurrencyType());
            assertEquals(0, savedAccount.getBalance().compareTo(MoneyNormalize(BigDecimal.ZERO)));
            assertEquals(AccountStatus.ACTIVE, savedAccount.getAccountStatus());
        }

        /**
         * 계좌 생성 실패 - 요청이 null인 경우
         */
        @Test
        void createAccount_fail_whenRequestIsNull() {
            expectCreateAccountException(null, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 중복된 계좌 번호
         */
        @Test
        void createAccount_fail_whenAccountNumberIsDuplicate() {
            when(accountNumberGeneratorService.generateAccountNumber()).thenReturn(testAccountNumber);
            when(accountRepository.existsByAccountNumber(testAccountNumber)).thenReturn(true);

            expectCreateAccountException(accountCreateRequestDTO, ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 계좌명 누락
         */
        @Test
        void createAccount_fail_whenAccountNameIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                .accountName(null)
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 계좌 유형 누락
         */
        @Test
        void createAccount_fail_whenAccountTypeIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                .accountName("mxxikr")
                .accountType(null)
                .currencyType(CurrencyType.KRW)
                .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 통화 종류 누락
         */
        @Test
        void createAccount_fail_whenCurrencyTypeIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                .accountName("mxxikr")
                .accountType(AccountType.PERSONAL)
                .currencyType(null)
                .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }
    }

    // ========================= 계좌 조회 테스트 =========================
    @Nested
    class GetAccountTest {

        /**
         * 계좌 조회 성공
         */
        @Test
        void getAccount_success() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(accountEntity));

            AccountResponseDTO result = accountService.getAccount(testAccountId);

            assertNotNull(result);
            assertEquals(testAccountId, result.getAccountId());
            assertEquals(testAccountNumber, result.getAccountNumber());
            verify(accountRepository).findById(testAccountId);
        }

        /**
         * 계좌 조회 실패 - 계좌가 존재하지 않는 경우
         */
        @Test
        void getAccount_fail_whenAccountNotFound() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

            TransferSystemException exception = assertThrows(TransferSystemException.class,
                    () -> accountService.getAccount(testAccountId));
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }
    }

    // ========================= 계좌 삭제 테스트 =========================
    @Nested
    class DeleteAccountTest {

        /**
         * 계좌 삭제 성공
         */
        @Test
        void deleteAccount_success() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(accountEntity));

            assertDoesNotThrow(() -> accountService.deleteAccount(testAccountId));
            verify(accountRepository).delete(accountEntity);
        }

        /**
         * 계좌 삭제 실패 - 계좌가 존재하지 않는 경우
         */
        @Test
        void deleteAccount_fail_whenAccountNotFound() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

            TransferSystemException exception = assertThrows(TransferSystemException.class,
                    () -> accountService.deleteAccount(testAccountId));
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
            verify(accountRepository, never()).delete(any());
        }
    }

    // ========================= 입금 테스트 =========================
    @Nested
    class DepositTest {

        /**
         * 입금 성공
         */
        @Test
        void deposit_success() {
            BigDecimal initialBalance = accountEntity.getBalance();
            BigDecimal depositAmount = new BigDecimal("50000");

            when(accountRepository.findByAccountNumberLock(testAccountNumber)).thenReturn(Optional.of(accountEntity));

            accountService.deposit(testAccountNumber, depositAmount);

            ArgumentCaptor<AccountEntity> accountCaptor = ArgumentCaptor.forClass(AccountEntity.class);

            verify(accountRepository).save(accountCaptor.capture());
            verify(transactionRepository).save(any(TransactionEntity.class));

            AccountEntity savedAccount = accountCaptor.getValue();
            assertEquals(0, savedAccount.getBalance().compareTo(initialBalance.add(depositAmount)));
        }

        /**
         * 입금 실패 - 계좌가 존재하지 않는 경우
         */
        @Test
        void deposit_fail_whenAccountNotFound() {
            when(accountRepository.findByAccountNumberLock("nonexistent")).thenReturn(Optional.empty());

            expectDepositException("nonexistent", new BigDecimal("50000"), ErrorCode.ACCOUNT_NOT_FOUND);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 입금 실패 - null 계좌번호
         */
        @Test
        void deposit_fail_whenAccountNumberIsNull() {
            expectDepositException(null, new BigDecimal("50000"), ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 입금 실패 - null 금액
         */
        @Test
        void deposit_fail_whenAmountIsNull() {
            expectDepositException(testAccountNumber, null, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 입금 실패 - 음수 금액
         */
        @Test
        void deposit_fail_whenAmountIsNegative() {
            expectDepositException(testAccountNumber, new BigDecimal("-1000"), ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 입금 실패 - 0원 금액
         */
        @Test
        void deposit_fail_whenAmountIsZero() {
            expectDepositException(testAccountNumber, BigDecimal.ZERO, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ========================= 출금 테스트 =========================
    @Nested
    class WithdrawTest {

        /**
         * 출금 성공
         */
        @Test
        void withdraw_success() {
            BigDecimal initialBalance = accountEntity.getBalance();
            BigDecimal withdrawAmount = new BigDecimal("30000");

            when(accountRepository.findByAccountNumberLock(testAccountNumber)).thenReturn(Optional.of(accountEntity));
            todayUsed(testAccountNumber, TransactionType.WITHDRAW, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateWithdrawAmount(any(BigDecimal.class), any(BigDecimal.class));

            accountService.withdraw(testAccountNumber, withdrawAmount);

            ArgumentCaptor<AccountEntity> accountCaptor = ArgumentCaptor.forClass(AccountEntity.class);
            verify(accountRepository).save(accountCaptor.capture());
            verify(transactionRepository).save(any(TransactionEntity.class));

            AccountEntity savedAccount = accountCaptor.getValue();

            assertEquals(0, savedAccount.getBalance().compareTo(initialBalance.subtract(withdrawAmount)));
        }

        /**
         * 출금 실패 - 계좌가 존재하지 않는 경우
         */
        @Test
        void withdraw_fail_whenAccountNotFound() {
            when(accountRepository.findByAccountNumberLock("nonexistent")).thenReturn(Optional.empty());

            expectWithdrawException("nonexistent", new BigDecimal("30000"), ErrorCode.ACCOUNT_NOT_FOUND);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - 잔액 부족
         */
        @Test
        void withdraw_fail_whenInsufficientBalance() {
            when(accountRepository.findByAccountNumberLock(testAccountNumber)).thenReturn(Optional.of(accountEntity));
            todayUsed(testAccountNumber, TransactionType.WITHDRAW, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateWithdrawAmount(any(BigDecimal.class), any(BigDecimal.class));

            expectWithdrawException(testAccountNumber, new BigDecimal("200000"), ErrorCode.INSUFFICIENT_BALANCE);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - 일일 한도 초과
         */
        @Test
        void withdraw_fail_whenExceedsDailyLimit() {
            BigDecimal withdrawAmount = new BigDecimal("600000");
            BigDecimal todayTotal = new BigDecimal("500000");

            when(accountRepository.findByAccountNumberLock(testAccountNumber)).thenReturn(Optional.of(accountEntity));
            todayUsed(testAccountNumber, TransactionType.WITHDRAW, todayTotal);

            doThrow(new TransferSystemException(ErrorCode.EXCEEDS_WITHDRAW_LIMIT))
                    .when(transferPolicy).validateWithdrawAmount(eq(withdrawAmount), eq(todayTotal));

            expectWithdrawException(testAccountNumber, withdrawAmount, ErrorCode.EXCEEDS_WITHDRAW_LIMIT);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - null 계좌번호
         */
        @Test
        void withdraw_fail_whenAccountNumberIsNull() {
            expectWithdrawException(null, new BigDecimal("30000"), ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - null 금액
         */
        @Test
        void withdraw_fail_whenAmountIsNull() {
            expectWithdrawException(testAccountNumber, null, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - 음수 금액
         */
        @Test
        void withdraw_fail_whenAmountIsNegative() {
            expectWithdrawException(testAccountNumber, new BigDecimal("-1000"), ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - 0원 금액
         */
        @Test
        void withdraw_fail_whenAmountIsZero() {
            expectWithdrawException(testAccountNumber, BigDecimal.ZERO, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }
    }
}