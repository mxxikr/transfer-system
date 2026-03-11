package com.transfer.system.controller;

import com.transfer.system.annotation.SuccessResponse;
import com.transfer.system.dto.*;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.service.AccountService;
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

    @Operation(summary = "계좌 생성")
    @PostMapping("/create")
    @SuccessResponse(ResponseMessage.ACCOUNT_CREATED)
    public AccountResponseDTO createAccount(@Valid @RequestBody AccountCreateRequestDTO AccountCreateRequestDTO) {
        return accountService.createAccount(AccountCreateRequestDTO);
    }

    @Operation(summary = "계좌 조회")
    @GetMapping("/{accountId}")
    @SuccessResponse(ResponseMessage.ACCOUNT_RETRIEVED)
    public AccountResponseDTO getAccount(@PathVariable UUID accountId) {
        return accountService.getAccount(accountId);
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
        return accountService.deposit(accountBalanceRequestDTO.getAccountNumber(), accountBalanceRequestDTO.getAmount());
    }

    @Operation(summary = "출금 처리", description = "일 한도 : 1,000,000원")
    @PostMapping("/withdraw")
    @SuccessResponse(ResponseMessage.WITHDRAW_SUCCESSFUL)
    public AccountBalanceResponseDTO withdraw(@Valid @RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        return accountService.withdraw(accountBalanceRequestDTO.getAccountNumber(), accountBalanceRequestDTO.getAmount());
    }
}
