
package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.PagingPolicy;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import com.transfer.system.utils.TimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    @Mock
    private PagingPolicy pagingPolicy;

    private TransactionServiceImpl transactionService;
    private TransactionRequestDTO transactionRequestDTO;
    private AccountEntity fromAccountEntity;
    private AccountEntity toAccountEntity;
    private TransactionEntity transactionEntity;

    private final String testFromAccountNumber = "00125080800001";
    private final String testToAccountNumber = "00125080800002";


    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(accountRepository, transactionRepository, transferPolicy, pagingPolicy);

        transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber(testFromAccountNumber)
            .toAccountNumber(testToAccountNumber)
            .amount(new BigDecimal("100000"))
            .build();

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
            .createdTimeStamp(TimeUtils.nowKstLocalDateTime())
            .build();
    }

    // ========================== 공통 메서드 =========================

    /**
     * 이체 시 예외 처리
     */
    private void expectTransferException(TransactionRequestDTO dto, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> transactionService.transfer(dto));
        assertEquals(expectedError, exception.getErrorCode());
    }

    /**
     * 거래 내역 조회 시 예외 처리
     */
    private void expectHistoryException(String accountNumber, int page, int size, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> transactionService.getTransactionHistory(accountNumber, page, size));
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
            BigDecimal fee = new BigDecimal("1000");

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(fee);

            todayUsed(testFromAccountNumber, TransactionType.TRANSFER, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateTransferAmount(any(BigDecimal.class), any(BigDecimal.class));
            when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(transactionEntity);

            transactionService.transfer(transactionRequestDTO);

            ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
            verify(transactionRepository).save(transactionCaptor.capture());

            TransactionEntity savedTransaction = transactionCaptor.getValue();

            assertEquals(fromAccountEntity, savedTransaction.getFromAccount());
            assertEquals(toAccountEntity, savedTransaction.getToAccount());
            assertEquals(transactionRequestDTO.getAmount(), savedTransaction.getAmount());
            assertEquals(fee, savedTransaction.getFee());
        }
        
        /**
         * 이체 성공 - 입, 출금 계좌 반대
         */
        @Test
        void transfer_success_reverse() {
            TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .fromAccountNumber(testToAccountNumber)
                .toAccountNumber(testFromAccountNumber)
                .amount(new BigDecimal("50000"))
                .build();

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("1000"));
            todayUsed(testToAccountNumber, TransactionType.TRANSFER, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateTransferAmount(any(BigDecimal.class), any(BigDecimal.class));
            when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(transactionEntity);

            TransactionResponseDTO result = transactionService.transfer(transactionRequestDTO);

            assertNotNull(result);
            verify(accountRepository, times(2)).findByAccountNumberLock(anyString());
            verify(transactionRepository).save(any(TransactionEntity.class));
        }

        /**
         * 이체 시 동일 계좌 간 이체
         */
        @Test
        void transfer_sameAccount() {
            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber(testFromAccountNumber)
                .toAccountNumber(testFromAccountNumber)
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferException(dto, ErrorCode.TRANSFER_SAME_ACCOUNT);
        }

        /**
         * 이체 시 출금 계좌 없음
         */
        @Test
        void transfer_fromAccountNotFound() {
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(accountRepository.findByAccountNumberLock("nonexistent")).thenReturn(Optional.empty());

            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber("nonexistent")
                .toAccountNumber(testToAccountNumber)
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferException(dto, ErrorCode.ACCOUNT_NOT_FOUND);
        }

        /**
         * 이체 시 입금 계좌 없음
         */
        @Test
        void transfer_toAccountNotFound() {
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("nonexistent")).thenReturn(Optional.empty());

            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber(testFromAccountNumber)
                .toAccountNumber("nonexistent")
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferException(dto, ErrorCode.ACCOUNT_NOT_FOUND);
        }

        /**
         * 이체 시 송신 계좌 비활성
         */
        @Test
        void transfer_fromInactiveAccount() {
            AccountEntity inactiveFromAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .accountNumber(testFromAccountNumber)
                .accountName("sender")
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("200000"))
                .accountStatus(AccountStatus.INACTIVE)
                .build();

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(inactiveFromAccount));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));

            expectTransferException(transactionRequestDTO, ErrorCode.SENDER_ACCOUNT_INACTIVE);
        }

        /**
         * 이체 시 수신 계좌 비활성
         */
        @Test
        void transfer_toInactiveAccount() {
            AccountEntity inactiveToAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .accountNumber(testToAccountNumber)
                .accountName("receiver")
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("100000"))
                .accountStatus(AccountStatus.INACTIVE)
                .build();

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(inactiveToAccount));

            expectTransferException(transactionRequestDTO, ErrorCode.RECEIVER_ACCOUNT_INACTIVE);
        }

        /**
         * 이체 시 통화 불일치
         */
        @Test
        void transfer_currencyMismatch() {
            AccountEntity usdAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .accountNumber(testToAccountNumber)
                .accountName("receiver")
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.USD)
                .balance(new BigDecimal("100000"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(usdAccount));

            expectTransferException(transactionRequestDTO, ErrorCode.CURRENCY_TYPE_MISMATCH);
        }

        /**
         * 이체 시 수수료 null 처리
         */
        @Test
        void transfer_nullFee() {
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(null);

            expectTransferException(transactionRequestDTO, ErrorCode.INVALID_FEE);
        }

        /**
         * 이체 시 수수료 음수 처리
         */
        @Test
        void transfer_negativeFee() {
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("-100"));

            expectTransferException(transactionRequestDTO, ErrorCode.INVALID_FEE);
        }

        /**
         * 이체 시 이체 한도 초과
         */
        @Test
        void transfer_limitExceeded() {
            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("1000"));

            todayUsed(testFromAccountNumber, TransactionType.TRANSFER, BigDecimal.ZERO);
            doThrow(new TransferSystemException(ErrorCode.TRANSFER_LIMIT_EXCEEDED)).when(transferPolicy).validateTransferAmount(any(BigDecimal.class), any(BigDecimal.class));

            expectTransferException(transactionRequestDTO, ErrorCode.TRANSFER_LIMIT_EXCEEDED);
        }

        /**
         * 이체 시 잔액 부족
         */
        @Test
        void transfer_insufficientBalance() {
            AccountEntity poorAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .accountNumber(testFromAccountNumber)
                .accountName("sender")
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("50000"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

            when(accountRepository.findByAccountNumberLock(testFromAccountNumber)).thenReturn(Optional.of(poorAccount));
            when(accountRepository.findByAccountNumberLock(testToAccountNumber)).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("1000"));

            todayUsed(testFromAccountNumber, TransactionType.TRANSFER, BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateTransferAmount(any(BigDecimal.class), any(BigDecimal.class));

            expectTransferException(transactionRequestDTO, ErrorCode.INSUFFICIENT_BALANCE);
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
            Page<TransactionEntity> transactionPage = new PageImpl<>(transactions, PageRequest.of(0, 10), transactions.size());

            when(accountRepository.findByAccountNumber(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(pagingPolicy.getValidatedPage(0)).thenReturn(0);
            when(pagingPolicy.getValidatedSize(10)).thenReturn(10);
            when(pagingPolicy.getTransactionSortField()).thenReturn("createdTimeStamp");
            when(transactionRepository.findAllByAccount(eq(fromAccountEntity), any(Pageable.class))).thenReturn(transactionPage);

            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(testFromAccountNumber, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(transactionRepository).findAllByAccount(eq(fromAccountEntity), pageableCaptor.capture());

            Pageable capturedPageable = pageableCaptor.getValue();
            assertEquals(0, capturedPageable.getPageNumber());
            assertEquals(10, capturedPageable.getPageSize());
            assertEquals(Sort.by("createdTimeStamp").descending(), capturedPageable.getSort());
        }

        /**
         * 거래 내역 조회 시 null 계좌번호
         */
        @Test
        void getTransactionHistory_nullAccountNumber() {
            expectHistoryException(null, 0, 10, ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        /**
         * 거래 내역 조회 시 빈 계좌번호
         */
        @Test
        void getTransactionHistory_emptyAccountNumber() {
            expectHistoryException("", 0, 10, ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        /**
         * 거래 내역 조회 시 계좌가 존재하지 않는 경우
         */
        @Test
        void getTransactionHistory_accountNotFound() {
            when(accountRepository.findByAccountNumber("nonexistent")).thenReturn(Optional.empty());

            expectHistoryException("nonexistent", 0, 10, ErrorCode.ACCOUNT_NOT_FOUND);
        }

        /**
         * 거래 내역 조회 시 음수 페이지 처리
         */
        @Test
        void getTransactionHistory_negativePage() {
            List<TransactionEntity> transactions = List.of(transactionEntity);
            Page<TransactionEntity> transactionPage = new PageImpl<>(transactions, PageRequest.of(0, 10), transactions.size());

            when(accountRepository.findByAccountNumber(testFromAccountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(pagingPolicy.getValidatedPage(null)).thenReturn(0);
            when(pagingPolicy.getValidatedSize(10)).thenReturn(10);
            when(pagingPolicy.getTransactionSortField()).thenReturn("createdTimeStamp");
            when(transactionRepository.findAllByAccount(eq(fromAccountEntity), any(Pageable.class))).thenReturn(transactionPage);

            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(testFromAccountNumber, -1, 10);
            assertNotNull(result);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(transactionRepository).findAllByAccount(eq(fromAccountEntity), pageableCaptor.capture());
            assertEquals(0, pageableCaptor.getValue().getPageNumber());
        }
    }
}