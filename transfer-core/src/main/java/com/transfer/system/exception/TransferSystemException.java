package com.transfer.system.exception;

import lombok.Getter;

@Getter
public class TransferSystemException extends RuntimeException {

    private final ErrorCode errorCode;

    public TransferSystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public TransferSystemException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}