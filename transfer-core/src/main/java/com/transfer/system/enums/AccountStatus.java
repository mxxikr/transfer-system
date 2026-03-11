package com.transfer.system.enums;

import lombok.Getter;

@Getter
public enum AccountStatus {
    ACTIVE("활성화"),
    INACTIVE("비활성화"),
    SUSPENDED("정지");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }
}