package com.transfer.system.annotation;

import com.transfer.system.enums.ResponseMessage;
import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SuccessResponse {
    ResponseMessage value() default ResponseMessage.SUCCESS;
}
