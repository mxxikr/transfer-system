package com.transfer.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequestDTO {
    @NotBlank
    private String fromAccountNumber; // 송금 계좌 번호

    @NotBlank
    private String toAccountNumber; // 수신 계좌 번호

    @NotNull
    @Positive
    private BigDecimal amount; // 송금 금액
}