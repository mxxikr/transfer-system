package com.transfer.system.domain;

import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.utils.MoneyUtils;
import com.transfer.system.utils.TimeUtils;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account_entity")
@Builder
public class AccountEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID accountId; // 계좌 고유 식별자

    @Column(nullable = false, unique = true)
    private String accountNumber; // 계좌 번호

    private String accountName; // 계좌 사용자명

    private String bankName; // 은행 이름

    @Enumerated(EnumType.STRING)
    private AccountType accountType; // 계좌 유형

    @Enumerated(EnumType.STRING)
    private CurrencyType currencyType; // 통화 종류

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balance; // 계좌 잔액

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus; // 계좌 상태

    // 출금
    public void subtractBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) { // 잔액 부족 여부 확인
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = MoneyUtils.normalize(this.balance.subtract(amount));
    }

    // 입금
    public void addBalance(BigDecimal amount) {
        this.balance = MoneyUtils.normalize(this.balance.add(amount));
    }

    // 계좌 잔액 업데이트
    public void updateBalance(BigDecimal newBalance) {
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.NEGATIVE_BALANCE);
        }
        this.balance = MoneyUtils.normalize(newBalance);
    }
}