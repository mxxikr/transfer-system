package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import com.transfer.system.utils.MoneyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private AccountEntity fromAccountEntity;
    private AccountEntity toAccountEntity;
    private TransactionEntity transactionEntity;

    private final String testFromAccountNumber = "00125080800001";
    private final String testToAccountNumber = "00125080800002";

    @BeforeEach
    void setUp() {
        fromAccountEntity = AccountEntity.builder()
            .accountId(UUID.randomUUID())
            .accountNumber(testFromAccountNumber)
            .balance(MoneyUtils.normalize(new BigDecimal("1000000")))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        toAccountEntity = AccountEntity.builder()
            .accountId(UUID.randomUUID())
            .accountNumber(testToAccountNumber)
            .balance(MoneyUtils.normalize(new BigDecimal("500000")))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
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

    // ========================== 이체 실행 테스트 (executeTransfer) =========================
    @Nested
    class ExecuteTransferTest {

        @Test
        void executeTransfer_success() {
            BigDecimal amount = new BigDecimal("100000");
            BigDecimal fee = new BigDecimal("1000");
            BigDecimal total = amount.add(fee);
            
            when(transferPolicy.calculateFee(amount)).thenReturn(fee);
            when(transactionRepository.getSumTodayUsedAmount(anyString(), eq(TransactionType.TRANSFER), any(), any()))
                .thenReturn(BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateTransferAmount(any(), any());
            when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(transactionEntity);

            TransactionEntity result = transactionService.executeTransfer(fromAccountEntity, toAccountEntity, amount);

            assertNotNull(result);
            assertThat(fromAccountEntity.getBalance()).isEqualByComparingTo(new BigDecimal("899000"));
            assertThat(toAccountEntity.getBalance()).isEqualByComparingTo(new BigDecimal("600000"));
            verify(transactionRepository).save(any(TransactionEntity.class));
        }

        @Test
        void executeTransfer_insufficientBalance() {
            BigDecimal amount = new BigDecimal("2000000");
            when(transferPolicy.calculateFee(amount)).thenReturn(new BigDecimal("20000"));
            when(transactionRepository.getSumTodayUsedAmount(anyString(), eq(TransactionType.TRANSFER), any(), any()))
                .thenReturn(BigDecimal.ZERO);

            assertThrows(TransferSystemException.class, () -> 
                transactionService.executeTransfer(fromAccountEntity, toAccountEntity, amount));
        }
    }

    // ========================= 입금 테스트 =========================
    @Nested
    class DepositTest {
        @Test
        void deposit_success() {
            BigDecimal amount = new BigDecimal("50000");
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(transactionRepository.save(any())).thenReturn(transactionEntity);

            TransactionEntity result = transactionService.deposit(testFromAccountNumber, amount);

            assertNotNull(result);
            assertThat(fromAccountEntity.getBalance()).isEqualByComparingTo(new BigDecimal("1050000"));
        }
    }

    // ========================= 출금 테스트 =========================
    @Nested
    class WithdrawTest {
        @Test
        void withdraw_success() {
            BigDecimal amount = new BigDecimal("10000");
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(transactionRepository.getSumTodayUsedAmount(anyString(), eq(TransactionType.WITHDRAW), any(), any()))
                .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.save(any())).thenReturn(transactionEntity);

            TransactionEntity result = transactionService.withdraw(testFromAccountNumber, amount);

            assertNotNull(result);
            assertThat(fromAccountEntity.getBalance()).isEqualByComparingTo(new BigDecimal("990000"));
        }
    }

    // ========================= 거래 내역 조회 테스트 =========================
    @Nested
    class GetTransactionHistoryTest {
        @Test
        void getTransactionHistory_success() {
            Page<TransactionEntity> transactionPage = new PageImpl<>(List.of(transactionEntity));

            when(accountRepository.findByAccountNumber(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(transactionRepository.findAllByAccountWithFetchJoin(eq(fromAccountEntity), any(Pageable.class))).thenReturn(transactionPage);

            Page<TransactionEntity> result = transactionService.getTransactionHistory(testFromAccountNumber, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(transactionRepository).findAllByAccountWithFetchJoin(eq(fromAccountEntity), any(Pageable.class));
        }
    }
}
