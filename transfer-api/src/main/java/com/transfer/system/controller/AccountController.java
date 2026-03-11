package com.transfer.system.controller;

import com.transfer.system.annotation.SuccessResponse;
import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.*;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.service.AccountService;
import com.transfer.system.service.TransactionService;
import com.transfer.system.utils.MoneyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "계좌 API", description = "계좌 생성, 조회, 삭제, 입출금 관련 API")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final TransactionService transactionService;

    @Operation(summary = "계좌 생성")
    @PostMapping("/create")
    @SuccessResponse(ResponseMessage.ACCOUNT_CREATED)
    public AccountResponseDTO createAccount(@Valid @RequestBody AccountCreateRequestDTO accountCreateRequestDTO) {
        AccountEntity account = accountService.createAccount(
            accountCreateRequestDTO.getAccountName(),
            accountCreateRequestDTO.getAccountType(),
            accountCreateRequestDTO.getCurrencyType()
        );
        return toAccountDto(account);
    }

    @Operation(summary = "계좌 조회")
    @GetMapping("/{accountId}")
    @SuccessResponse(ResponseMessage.ACCOUNT_RETRIEVED)
    public AccountResponseDTO getAccount(@PathVariable UUID accountId) {
        AccountEntity account = accountService.getAccount(accountId);
        return toAccountDto(account);
    }

    @Operation(summary = "계좌 삭제")
    @DeleteMapping("/{accountId}")
    @SuccessResponse(ResponseMessage.ACCOUNT_DELETED)
    public void deleteAccount(@PathVariable UUID accountId) {
        accountService.deleteAccount(accountId);
    }

    @Operation(summary = "입금 처리")
    @PostMapping("/deposit")
    @SuccessResponse(ResponseMessage.DEPOSIT_SUCCESSFUL)
    public AccountBalanceResponseDTO deposit(@Valid @RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        TransactionEntity transaction = transactionService.deposit(
            accountBalanceRequestDTO.getAccountNumber(),
            accountBalanceRequestDTO.getAmount()
        );
        return toAccountBalanceDto(transaction.getToAccount(), transaction);
    }

    @Operation(summary = "출금 처리", description = "일 한도 : 1,000,000원")
    @PostMapping("/withdraw")
    @SuccessResponse(ResponseMessage.WITHDRAW_SUCCESSFUL)
    public AccountBalanceResponseDTO withdraw(@Valid @RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        TransactionEntity transaction = transactionService.withdraw(
            accountBalanceRequestDTO.getAccountNumber(),
            accountBalanceRequestDTO.getAmount()
        );
        return toAccountBalanceDto(transaction.getFromAccount(), transaction);
    }

    private AccountResponseDTO toAccountDto(AccountEntity e) {
        return AccountResponseDTO.builder()
            .accountId(e.getAccountId())
            .accountNumber(e.getAccountNumber())
            .accountName(e.getAccountName())
            .bankName(e.getBankName())
            .accountType(e.getAccountType())
            .currencyType(e.getCurrencyType())
            .balance(MoneyUtils.normalize(e.getBalance()))
            .accountStatus(e.getAccountStatus())
            .createdTimeStamp(e.getCreatedTimeStamp())
            .updatedTimeStamp(e.getUpdatedTimeStamp())
            .build();
    }

    private AccountBalanceResponseDTO toAccountBalanceDto(AccountEntity account, TransactionEntity transaction) {
        return AccountBalanceResponseDTO.builder()
            .accountNumber(account.getAccountNumber())
            .amount(MoneyUtils.normalize(transaction.getAmount()))
            .balance(MoneyUtils.normalize(account.getBalance()))
            .build();
    }
}
