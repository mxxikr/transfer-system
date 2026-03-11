package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferFacadeTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransferFacade transferFacade;

    @Test
    void transfer_shouldLockInOrder_toPreventDeadlock() {
        // Given
        String accountA = "001";
        String accountB = "002";
        BigDecimal amount = new BigDecimal("1000");

        AccountEntity entityA = AccountEntity.builder().accountNumber(accountA).build();
        AccountEntity entityB = AccountEntity.builder().accountNumber(accountB).build();

        when(accountRepository.findByAccountNumberLock("001")).thenReturn(Optional.of(entityA));
        when(accountRepository.findByAccountNumberLock("002")).thenReturn(Optional.of(entityB));
        when(transactionService.executeTransfer(any(), any(), any())).thenReturn(mock(TransactionEntity.class));

        // When
        transferFacade.transfer(accountB, accountA, amount);

        // Then
        var inOrder = inOrder(accountRepository);
        inOrder.verify(accountRepository).findByAccountNumberLock("001");
        inOrder.verify(accountRepository).findByAccountNumberLock("002");
        
        verify(transactionService).executeTransfer(entityB, entityA, amount);
    }

    @Test
    void transfer_shouldThrowException_whenSameAccount() {
        assertThrows(TransferSystemException.class, () -> 
            transferFacade.transfer("001", "001", new BigDecimal("1000")));
    }
}
