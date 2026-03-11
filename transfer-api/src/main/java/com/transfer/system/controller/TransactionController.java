package com.transfer.system.controller;

import com.transfer.system.annotation.SuccessResponse;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.service.TransactionService;
import com.transfer.system.service.TransferFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "거래 API", description = "계좌 간 이체 및 거래 내역 조회 API")
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;
    private final TransferFacade transferFacade;

    @Operation(summary = "계좌 이체", description = "이체 수수료 : 1%")
    @PostMapping("/transfer")
    @SuccessResponse(ResponseMessage.TRANSFER_SUCCESSFUL)
    public TransactionResponseDTO transfer(@Valid @RequestBody TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity result = transferFacade.transfer(
                transactionRequestDTO.getFromAccountNumber(),
                transactionRequestDTO.getToAccountNumber(),
                transactionRequestDTO.getAmount()
        );
        return toDto(result);
    }

    @Operation(summary = "거래 내역 조회", description = "거래 내역 최신 순 조회")
    @GetMapping("/history")
    @SuccessResponse(ResponseMessage.TRANSACTION_HISTORY_RETRIEVED)
    public Page<TransactionResponseDTO> getTransactionHistory(@RequestParam @NotBlank String accountNumber, @RequestParam int page, @RequestParam int size) {
        Page<TransactionEntity> history = transactionService.getTransactionHistory(accountNumber, page, size);
        return history.map(this::toDto);
    }

    private TransactionResponseDTO toDto(TransactionEntity e) {
        return TransactionResponseDTO.builder()
            .transactionId(e.getTransactionId())
            .fromAccountNumber(e.getFromAccount() != null ? e.getFromAccount().getAccountNumber() : null)
            .toAccountNumber(e.getToAccount() != null ? e.getToAccount().getAccountNumber() : null)
            .amount(e.getAmount())
            .fee(e.getFee())
            .transactionType(e.getTransactionType())
            .createdTimeStamp(e.getCreatedTimeStamp())
            .build();
    }
}
