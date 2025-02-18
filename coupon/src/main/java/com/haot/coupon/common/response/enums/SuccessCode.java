package com.haot.coupon.common.response.enums;

import com.haot.coupon.common.response.ResCodeIfs;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode implements ResCodeIfs {

    // 쿠폰
    CREATE_COUPON_SUCCESS(HttpStatus.CREATED, "4000", "쿠폰이 성공적으로 생성되었습니다."),
    CUSTOMER_ISSUED_COUPON_SUCCESS(HttpStatus.OK, "4000", "사용자가 쿠폰을 성공적으로 발급하였습니다."),
    GET_DETAIL_COUPON_SUCCESS(HttpStatus.OK, "4000", "쿠폰 상세조회 성공하였습니다."),
    GET_USER_COUPONS_SUCCESS(HttpStatus.OK, "4000", "쿠폰함 조회 성공했습니다."),

    // 쿠폰 Feign,
    VERIFY_COUPON_SUCCESS(HttpStatus.OK, "4000", "쿠폰 유효성 검사 성공하였습니다."),
    COUPON_RESERVATION_SUCCESS(HttpStatus.OK, "4000", "쿠폰 상태 변경 성공하였습니다."),
    COUPON_RESERVATION_ROLLBACK_SUCCESS(HttpStatus.OK, "4000", "쿠폰 Rollback 성공하였습니다."),

    // 이벤트
    CREATE_EVENT_SUCCESS(HttpStatus.CREATED, "4000", "이벤트가 성공적으로 생성되었습니다."),
    GET_DETAIL_EVENT_SUCCESS(HttpStatus.OK, "4000", "이벤트 상세조회 성공하였습니다."),
    MODIFY_EVENT_SUCCESS(HttpStatus.OK, "4000", "이벤트 성공적으로 수정하였습니다."),
    SEARCH_EVENT_SUCCESS(HttpStatus.OK, "4000", "이벤트 조회 성공하였습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
