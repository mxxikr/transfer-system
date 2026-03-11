package com.transfer.system.controller;

import com.transfer.system.annotation.SuccessResponse;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.policy.PagingPolicy;
import com.transfer.system.service.TransactionService;
import com.transfer.system.utils.MoneyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Tag(name = "거래 API", description = "계좌 간 이체 및 거래 내역 조회 API")
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;
    private final PagingPolicy pagingPolicy;

    @Operation(summary = "계좌 이체", description = "이체 수수료 : 1%")
    @PostMapping("/transfer")
    @SuccessResponse(ResponseMessage.TRANSFER_SUCCESSFUL)
    public TransactionResponseDTO transfer(@Valid @RequestBody TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity transaction = transactionService.transfer(
            transactionRequestDTO.getFromAccountNumber(),
            transactionRequestDTO.getToAccountNumber(),
            transactionRequestDTO.getAmount()
        );
        return toTransactionDto(transaction);
    }

    @Operation(summary = "거래 내역 조회", description = "거래 내역 최신 순 조회")
    @GetMapping("/history")
    @SuccessResponse(ResponseMessage.TRANSACTION_HISTORY_RETRIEVED)
    public Page<TransactionResponseDTO> getTransactionHistory(@RequestParam @NotBlank String accountNumber, @RequestParam int page, @RequestParam int size) {
        // API 레이어에서 페이징 파라미터 유효성 검사 수행
        int validatedPage = pagingPolicy.getValidatedPage(page >= 0 ? page : null);
        int validatedSize = pagingPolicy.getValidatedSize(size);

        Page<TransactionEntity> transactions = transactionService.getTransactionHistory(accountNumber, validatedPage, validatedSize);
        return transactions.map(this::toTransactionDto);
    }

    private TransactionResponseDTO toTransactionDto(TransactionEntity e) {
        String fromNumber = (e.getFromAccount() != null) ? e.getFromAccount().getAccountNumber() : null;
        String toNumber = (e.getToAccount() != null) ? e.getToAccount().getAccountNumber() : null;

        return TransactionResponseDTO.builder()
            .transactionId(e.getTransactionId())
            .fromAccountNumber(fromNumber)
            .toAccountNumber(toNumber)
            .amount(MoneyUtils.normalize(e.getAmount()))
            .fee(MoneyUtils.normalize(e.getFee()))
            .transactionType(e.getTransactionType())
            .createdTimeStamp(e.getCreatedTimeStamp())
            .build();
    }
}
