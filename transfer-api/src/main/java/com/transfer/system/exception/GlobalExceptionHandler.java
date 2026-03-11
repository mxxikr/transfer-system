package com.transfer.system.exception;

import com.transfer.system.dto.CommonResponseDTO;
import com.transfer.system.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice(basePackages = "com.transfer.system")
public class GlobalExceptionHandler {
    // 도메인, 비즈니스 예외
    @ExceptionHandler(TransferSystemException.class)
    public ResponseEntity<CommonResponseDTO<Void>> handleTransferException(TransferSystemException ex) {
        ErrorCode code = ex.getErrorCode();

        // ErrorCode에 따라 ResultCode를 결정
        ResultCode result = switch (code) {
            case INVALID_REQUEST,
                 INVALID_ACCOUNT_NUMBER,
                 INVALID_AMOUNT,
                 INVALID_FEE,
                 TRANSFER_SAME_ACCOUNT,
                 EXCEEDS_WITHDRAW_LIMIT,
                 CURRENCY_TYPE_MISMATCH -> ResultCode.FAIL_INVALID_PARAMETER;
            default -> ResultCode.FAIL_DATA_ERROR;
        };

        return ResponseEntity
            .status(code.getStatus())
            .body(CommonResponseDTO.failure(result, code.getMessage()));
    }

    // 서버 내부 오류 처리
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class
    })
    public ResponseEntity<CommonResponseDTO<Void>> handleServerLikeBadInputs(Exception ex) {
        log.warn("[INTERNAL_ERROR] {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(CommonResponseDTO.failure(ResultCode.ERROR_SERVER, ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    // 잘못된 요청 처리
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<CommonResponseDTO<Void>> handleBadRequest(Exception ex) {
        ErrorCode code = ErrorCode.INVALID_REQUEST;
        log.warn("[INVALID_REQUEST] {}", ex.getMessage());
        return ResponseEntity
                .status(code.getStatus())
                .body(CommonResponseDTO.failure(ResultCode.FAIL_INVALID_PARAMETER, code.getMessage()));
    }

    // 그 외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponseDTO<Void>> handleUnexpected(Exception ex) {
        log.error("[INTERNAL_ERROR] : {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(ErrorCode.INTERNAL_ERROR.getStatus())
            .body(CommonResponseDTO.failure(ResultCode.ERROR_SERVER, ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}