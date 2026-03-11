package com.transfer.system.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    DEPOSIT("입금"),
    WITHDRAW("출금"),
    TRANSFER("이체");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }
}