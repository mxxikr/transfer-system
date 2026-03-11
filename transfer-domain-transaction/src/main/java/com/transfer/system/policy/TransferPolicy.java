package com.transfer.system.policy;

import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.utils.MoneyUtils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Component
public class TransferPolicy {
    private final BigDecimal feeRate; // 수수료율
    private final BigDecimal withdrawDailyLimit; // 출금 일일 한도
    private final BigDecimal transferDailyLimit; // 이체 일일 한도

    public TransferPolicy(
        @Value("${transfer.fee-rate}") BigDecimal feeRate,
        @Value("${transfer.withdraw-daily-limit}") BigDecimal withdrawDailyLimit,
        @Value("${transfer.transfer-daily-limit}") BigDecimal transferDailyLimit) {

        if (feeRate == null || feeRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.INTERNAL_ERROR);
        }
        if (withdrawDailyLimit == null || withdrawDailyLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.INTERNAL_ERROR);
        }
        if (transferDailyLimit == null || transferDailyLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.INTERNAL_ERROR);
        }

        this.feeRate = feeRate;
        this.withdrawDailyLimit = withdrawDailyLimit;
        this.transferDailyLimit = transferDailyLimit;
    }

    /**
     * 수수료 계산
     */
    public BigDecimal calculateFee(BigDecimal amount) {
        return MoneyUtils.normalize(amount.multiply(feeRate));
    }

    /**
     * 출금 금액 검증
     */
    public void validateWithdrawAmount(BigDecimal amount, BigDecimal todayWithdrawTotal) {
        if (todayWithdrawTotal.add(amount).compareTo(withdrawDailyLimit) > 0) {
            throw new TransferSystemException(ErrorCode.EXCEEDS_WITHDRAW_LIMIT);
        }
    }

    /**
     * 이체 금액 검증
     */
    public void validateTransferAmount(BigDecimal amount, BigDecimal todayTransferTotal) {
        if (todayTransferTotal.add(amount).compareTo(transferDailyLimit) > 0) {
            throw new TransferSystemException(ErrorCode.TRANSFER_LIMIT_EXCEEDED);
        }
    }
}