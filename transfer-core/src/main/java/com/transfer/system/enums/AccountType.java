package com.transfer.system.enums;

import lombok.Getter;

@Getter
public enum AccountType {
    PERSONAL("개인"),
    BUSINESS("사업자");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }
}