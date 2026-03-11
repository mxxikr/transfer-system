package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.utils.MoneyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    private AccountNumberGeneratorService accountNumberGeneratorService;

    private AccountServiceImpl accountService;

    private AccountEntity accountEntity;
    private final UUID testAccountId = UUID.randomUUID();
    private final String testAccountNumber = "00125080800001";

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, accountNumberGeneratorService);

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

        lenient().when(accountRepository.save(any(AccountEntity.class))).thenAnswer(inv -> inv.getArgument(0));
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
    private void expectCreateAccountException(String name, AccountType type, CurrencyType currency, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> accountService.createAccount(name, type, currency));
        assertEquals(expectedError, exception.getErrorCode());
    }

    // ========================= 계좌 생성 테스트 =========================
    @Nested
    class CreateAccountTest {

        /**
         * 계좌 생성 성공
         */
        @Test
        void createAccount_success() {
            String name = "mxxikr";
            AccountType type = AccountType.PERSONAL;
            CurrencyType currency = CurrencyType.KRW;

            when(accountNumberGeneratorService.generateAccountNumber()).thenReturn(testAccountNumber);
            when(accountRepository.existsByAccountNumber(testAccountNumber)).thenReturn(false);

            accountService.createAccount(name, type, currency);

            ArgumentCaptor<AccountEntity> accountCaptor = ArgumentCaptor.forClass(AccountEntity.class);
            verify(accountRepository).save(accountCaptor.capture());

            AccountEntity savedAccount = accountCaptor.getValue();

            assertEquals(testAccountNumber, savedAccount.getAccountNumber());
            assertEquals(name, savedAccount.getAccountName());
            assertEquals(type, savedAccount.getAccountType());
            assertEquals(currency, savedAccount.getCurrencyType());
            assertEquals(0, savedAccount.getBalance().compareTo(MoneyNormalize(BigDecimal.ZERO)));
            assertEquals(AccountStatus.ACTIVE, savedAccount.getAccountStatus());
        }

        /**
         * 계좌 생성 실패 - 중복된 계좌 번호
         */
        @Test
        void createAccount_fail_whenAccountNumberIsDuplicate() {
            String name = "mxxikr";
            AccountType type = AccountType.PERSONAL;
            CurrencyType currency = CurrencyType.KRW;

            when(accountNumberGeneratorService.generateAccountNumber()).thenReturn(testAccountNumber);
            when(accountRepository.existsByAccountNumber(testAccountNumber)).thenReturn(true);

            expectCreateAccountException(name, type, currency, ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
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

            AccountEntity result = accountService.getAccount(testAccountId);

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
            // 삭제를 위해서는 ACTIVE 상태가 아니어야 함 (AccountServiceImpl 로직 확인)
            accountEntity.setAccountStatus(AccountStatus.INACTIVE);
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(accountEntity));

            assertDoesNotThrow(() -> accountService.deleteAccount(testAccountId));
            verify(accountRepository).delete(accountEntity);
        }

        /**
         * 계좌 삭제 실패 - 활성 계좌인 경우
         */
        @Test
        void deleteAccount_fail_whenAccountIsActive() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(accountEntity));

            TransferSystemException exception = assertThrows(TransferSystemException.class,
                    () -> accountService.deleteAccount(testAccountId));
            assertEquals(ErrorCode.ACCOUNT_CANNOT_BE_DELETED, exception.getErrorCode());
            verify(accountRepository, never()).delete(any());
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
}
