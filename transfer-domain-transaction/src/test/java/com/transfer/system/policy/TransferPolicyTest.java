package com.transfer.system.policy;

import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class TransferPolicyTest {

    private TransferPolicy policy;

    private static final BigDecimal FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal WITHDRAW_LIMIT = new BigDecimal("1000000");
    private static final BigDecimal TRANSFER_LIMIT = new BigDecimal("3000000");

    @BeforeEach
    void setUp() {
        policy = new TransferPolicy(FEE_RATE, WITHDRAW_LIMIT, TRANSFER_LIMIT);
    }

    // ========================== 공통 메서드 =========================

    private void expectWithdrawException(BigDecimal amount, BigDecimal today, ErrorCode code) {
        TransferSystemException ex = assertThrows(TransferSystemException.class,
                () -> policy.validateWithdrawAmount(amount, today)
        );
        assertEquals(code, ex.getErrorCode());
    }

    private void expectTransferException(BigDecimal amount, BigDecimal today, ErrorCode code) {
        TransferSystemException ex = assertThrows(TransferSystemException.class,
                () -> policy.validateTransferAmount(amount, today)
        );
        assertEquals(code, ex.getErrorCode());
    }

    // ========================== 수수료 계산 ==========================
    @Nested

    class CalculateFeeTest {

        /**
         * 수수료 계산
         */
        @Test
        void calculateFee_roundsHalfUp() {
            BigDecimal fee = policy.calculateFee(new BigDecimal("12345.67"));
            assertEquals(new BigDecimal("123.46"), fee);
            assertEquals(2, fee.scale());
        }

        /**
         * 수수료 계산 - 소수점 이하가 없는 경우
         */
        @Test
        void calculateFee_zeroAmount() {
            BigDecimal fee = policy.calculateFee(BigDecimal.ZERO);
            assertEquals(new BigDecimal("0.00"), fee);
        }
    }

    // ========================== 출금 한도 테스트 ==========================
    @Nested
    class WithdrawLimitTest {

        /**
         * 출금 금액이 한도 아래
         */
        @Test
        void underLimit() {
            assertDoesNotThrow(() -> policy.validateWithdrawAmount(
                    new BigDecimal("500000"), new BigDecimal("300000")));
        }

        /**
         * 출금 금액이 한도와 같음
         */
        @Test
        void equalToLimit() {
            assertDoesNotThrow(() -> policy.validateWithdrawAmount(
                    new BigDecimal("400000"), new BigDecimal("600000")));
        }

        /**
         * 출금 금액이 한도를 초과함
         */
        @Test
        void exceedLimit_Throws() {
            expectWithdrawException(new BigDecimal("500000"), new BigDecimal("600000"),
                    ErrorCode.EXCEEDS_WITHDRAW_LIMIT);
        }
    }

    // ========================== 이체 한도 ==========================
    @Nested
    class TransferLimitTest {

        /**
         * 이체 금액이 한도 아래
         */
        @Test
        void underLimit() {
            assertDoesNotThrow(() -> policy.validateTransferAmount(
                    new BigDecimal("500000"), new BigDecimal("2000000")));
        }

        /**
         * 이체 금액이 한도와 같음
         */
        @Test
        void equalToLimit() {
            assertDoesNotThrow(() -> policy.validateTransferAmount(
                    new BigDecimal("500000"), new BigDecimal("2500000")));
        }

        /**
         * 이체 금액이 한도를 초과함
         */
        @Test
        void exceedLimit_Throws() {
            expectTransferException(new BigDecimal("200000"), new BigDecimal("2900000"),
                    ErrorCode.TRANSFER_LIMIT_EXCEEDED);
        }
    }
}