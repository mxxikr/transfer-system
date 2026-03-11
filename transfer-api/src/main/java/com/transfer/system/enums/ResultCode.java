package com.transfer.system.enums;

import lombok.Getter;

@Getter
public enum ResultCode {
    // SUCCESS
    SUCCESS_HAS_DATA(1, "성공"),
    SUCCESS_NO_DATA(0, "데이터 없음"),

    // FAILURE
    FAIL_DATA_ERROR(-1, "데이터 조회 실패/오류"),
    FAIL_INVALID_PARAMETER(-2, "입력 파라미터 오류"),
    ERROR_SERVER(-3, "서버 오류");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}