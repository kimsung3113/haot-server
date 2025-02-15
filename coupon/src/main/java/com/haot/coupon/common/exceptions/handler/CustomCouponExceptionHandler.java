package com.haot.coupon.common.exceptions.handler;

import com.haot.coupon.common.exceptions.CustomCouponException;
import com.haot.coupon.common.response.ApiResponse;
import com.haot.coupon.common.response.ResCodeIfs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j(topic = "CustomCouponExceptionHandler")
@RestControllerAdvice
@Order(value = Integer.MIN_VALUE) // 최우선 처리
public class CustomCouponExceptionHandler {

    @ExceptionHandler(value = CustomCouponException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomCouponException(CustomCouponException customCouponException) {

        //log.error("{}", customCouponException.resCode.getMessage());
        ResCodeIfs errorCode = customCouponException.resCode;

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));

    }


}
