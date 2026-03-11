package com.transfer.system.enums;

import lombok.Getter;

@Getter
public enum CurrencyType {
    KRW("원화", "₩"),
    USD("달러", "$"),
    EUR("유로", "€"),
    JPY("엔화", "¥");

    private final String name;
    private final String symbol;

    CurrencyType(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }
}