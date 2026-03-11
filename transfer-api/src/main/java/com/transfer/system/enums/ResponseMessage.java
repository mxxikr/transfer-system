package com.transfer.system.enums;

import lombok.Getter;

@Getter
public enum ResponseMessage {
    SUCCESS("요청이 성공적으로 처리되었습니다."),
    // ACCOUNT
    ACCOUNT_CREATED("계좌 생성이 완료되었습니다."),
    ACCOUNT_RETRIEVED("계좌 조회가 완료되었습니다."),
    ACCOUNT_DELETED("계좌 삭제가 완료되었습니다."),
    DEPOSIT_SUCCESSFUL("입금이 완료되었습니다."),
    WITHDRAW_SUCCESSFUL("출금이 완료되었습니다."),

    // TRANSACTION
    TRANSFER_SUCCESSFUL("이체가 완료되었습니다."),
    TRANSACTION_HISTORY_RETRIEVED("거래 내역 조회가 완료되었습니다.");

    private final String message;

    ResponseMessage(String message) {
        this.message = message;
    }
}