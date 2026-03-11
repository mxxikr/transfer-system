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
public class AccountBalanceRequestDTO {
    @NotBlank
    private String accountNumber; // 계좌 번호

    @NotNull
    @Positive
    private BigDecimal amount; // 잔액
}