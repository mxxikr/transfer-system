package com.transfer.system.dto;


import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreateRequestDTO {
    @NotBlank
    private String accountName; // 계좌 사용자명

    @NotNull
    private AccountType accountType; // 계좌 유형
    @NotNull
    private CurrencyType currencyType; // 통화 유형
}