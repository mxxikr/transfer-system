package com.transfer.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.transfer.system.enums.ResultCode;
import com.transfer.system.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommonResponseDTO<T> {

    @JsonProperty("result_code")
    private final Integer resultCode;

    private final T data;

    private final String message;

    private final LocalDateTime timestamp;


    public static <T> CommonResponseDTO<T> successHasData(T data, String message) {
        return new CommonResponseDTO<>(
                ResultCode.SUCCESS_HAS_DATA.getCode(),
                data,
                message,
                TimeUtils.nowKstLocalDateTime()
        );
    }

    public static <T> CommonResponseDTO<T> successNoData(String message) {
        return new CommonResponseDTO<>(
                ResultCode.SUCCESS_NO_DATA.getCode(),
                null,
                message,
                TimeUtils.nowKstLocalDateTime()
        );
    }

    public static <T> CommonResponseDTO<T> failure(ResultCode resultCode) {
        return new CommonResponseDTO<>(
                resultCode.getCode(),
                null,
                resultCode.getMessage(),
                TimeUtils.nowKstLocalDateTime()
        );
    }

    public static <T> CommonResponseDTO<T> failure(ResultCode resultCode, String customMessage) {
        return new CommonResponseDTO<>(
                resultCode.getCode(),
                null,
                customMessage,
                TimeUtils.nowKstLocalDateTime()
        );
    }
}