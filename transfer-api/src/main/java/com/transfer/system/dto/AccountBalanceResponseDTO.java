package com.transfer.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceResponseDTO {
    private String accountNumber; // 계좌 번호
    private BigDecimal amount; // 거래 금액
    private BigDecimal balance; // 잔액
}