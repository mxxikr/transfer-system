package com.transfer.system.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
    private MoneyUtils() {}

    public static final int FEE_SCALE = 2; // 수수료 소수점 자리수
    public static final RoundingMode FEE_ROUNDING_MODE = RoundingMode.HALF_UP; // 수수료 반올림 방식;

    /**
     * 금액 정규화
     */
    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        return amount.setScale(FEE_SCALE, FEE_ROUNDING_MODE);
    }
}