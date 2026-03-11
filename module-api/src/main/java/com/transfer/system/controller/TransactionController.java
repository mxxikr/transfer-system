package com.transfer.system.controller;

import com.transfer.system.annotation.SuccessResponse;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

@Tag(name = "거래 API", description = "계좌 간 이체 및 거래 내역 조회 API")
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "계좌 이체", description = "이체 수수료 : 1%")
    @PostMapping("/transfer")
    @SuccessResponse(ResponseMessage.TRANSFER_SUCCESSFUL)
    public TransactionResponseDTO transfer(@Valid @RequestBody TransactionRequestDTO transactionRequestDTO) {
        return transactionService.transfer(transactionRequestDTO);
    }

    @Operation(summary = "거래 내역 조회", description = "거래 내역 최신 순 조회")
    @GetMapping("/history")
    @SuccessResponse(ResponseMessage.TRANSACTION_HISTORY_RETRIEVED)
    public Page<TransactionResponseDTO> getTransactionHistory(@RequestParam String accountNumber, @RequestParam int page, @RequestParam int size) {
        return transactionService.getTransactionHistory(accountNumber, page, size);
    }
}
