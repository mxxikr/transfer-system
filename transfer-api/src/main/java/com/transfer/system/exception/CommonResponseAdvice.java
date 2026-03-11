package com.transfer.system.exception;

import com.transfer.system.annotation.SuccessResponse;
import com.transfer.system.dto.CommonResponseDTO;
import com.transfer.system.enums.ResponseMessage;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.transfer.system.controller")
public class CommonResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.getParameterType().equals(CommonResponseDTO.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // 컨트롤러 메서드에서 @SuccessResponse 어노테이션 확인
        SuccessResponse successResponse = returnType.getMethodAnnotation(SuccessResponse.class);
        String message = (successResponse != null) ? successResponse.value().getMessage() : ResponseMessage.SUCCESS.getMessage();

        if (body == null) {
            return CommonResponseDTO.successNoData(message);
        }

        return CommonResponseDTO.successHasData(body, message);
    }
}
