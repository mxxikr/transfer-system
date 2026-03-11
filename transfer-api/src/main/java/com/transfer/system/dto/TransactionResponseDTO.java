package com.transfer.system.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.transfer.system.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponseDTO {
    private UUID transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private LocalDateTime createdTimeStamp;
}