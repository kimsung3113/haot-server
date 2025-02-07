package com.haot.coupon.common.response.enums;

import com.haot.coupon.common.response.ResCodeIfs;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements ResCodeIfs {

    // 0000: Common Error
    INTERNAL_SERVER_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "0000", "Unknown Server Error"),
    NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "0001", "존재하지 않는 리소스입니다."),
    VALIDATION_EXCEPTION(HttpStatus.BAD_REQUEST, "0009", "Validation failed"),

    // user exception
    USER_NOT_MATCHED(HttpStatus.FORBIDDEN,"4001" , "유저 ID 가 일치하지 않습니다."),

    // 쿠폰 exception
    DISCOUNT_RATE_EXCEPTION(HttpStatus.BAD_REQUEST, "4002", "할인율은 1 ~ 100 사이의 숫자여야 됩니다."),
    RESERVATION_STATUS_NOT_MATCH(HttpStatus.BAD_REQUEST, "4003", "reservationStatus NotMatch"),
    INSUFFICIENT_DATE_DIFFERENCE(HttpStatus.BAD_REQUEST, "4004", "시작날짜와 만료 날짜가 최소 하루이상 차이가 나야합니다."),
    TOO_MANY_DISCOUNT_POLICY(HttpStatus.BAD_REQUEST, "4005", "할인 정책은 하나만 적용할 수 있습니다."),
    DISCOUNT_POLICY_NOT_MATCH(HttpStatus.BAD_REQUEST, "4006", "할인 정책이 맞지 않습니다."),
    WRONG_DISCOUNT_AMOUNT(HttpStatus.BAD_REQUEST, "4007", "할인 금액 설정이 맞지 않거나 없습니다."),
    DISCOUNT_EXCEEDS_MIN_AMOUNT(HttpStatus.BAD_REQUEST, "4008", "할인 금액이 최소 사용 가능 금액을 초과합니다."),
    DISCOUNT_EXCEEDS_MAX_AMOUNT(HttpStatus.BAD_REQUEST, "4009", "할인 금액이 최대 사용 가능 금액을 초과합니다."),

    WRONG_TOTAL_QUANTITY(HttpStatus.BAD_REQUEST, "4010", "쿠폰 최대 발급 수량이 정책에 맞지 않습니다."),
    COUPON_TYPE_NOT_MATCH(HttpStatus.BAD_REQUEST, "4011", "쿠폰 타입이 맞지 않습니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "4012", "Coupon not found."),
    EXIST_PRIORITY_COUPON_EVENTS(HttpStatus.CONFLICT, "4013", "이미 있는 이벤트의 선착순 쿠폰입니다."),
    EXIST_UNLIMITED_COUPON_EVENTS(HttpStatus.CONFLICT, "4014", "이 무제한 쿠폰을 쓰는 이벤트가 종료되지 않았습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "4015", "Event not found."),
    DUPLICATED_ISSUED_COUPON(HttpStatus.BAD_REQUEST, "4016", "이미 발급 된 쿠폰입니다."),
    CURRENT_EVENT_NOT_STARTED(HttpStatus.BAD_REQUEST, "4017", "현재 이벤트가 시작하지 않았습니다."),
    EVENT_STATUS_NOT_MATCH(HttpStatus.BAD_REQUEST, "4018", "이벤트 상태가 맞지 않습니다."),
    COUPON_USED_WRONG_TIME(HttpStatus.CONFLICT, "4019", "현재 쿠폰을 사용할 수 없는 날짜입니다."),

    INVALID_PAYMENT_AMOUNT_FOR_COUPON(HttpStatus.BAD_REQUEST, "4020", "쿠폰 사용에 부적합한 결제 금액입니다."),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "4021", "이미 사용한 쿠폰입니다."),
    COUPON_UNAVAILABLE(HttpStatus.CONFLICT, "4022", "쿠폰이 사용 중이거나 만료되었거나 이미 선점된 상태입니다."),
    USER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "4023", "UserCoupon not found."),
    MODIFY_EVENT_HAS_NO_PARAMETER(HttpStatus.NOT_FOUND, "4024", "이벤트 수정 API 요청시 적어도 하나의 필드를 제공해야 합니다. "),
    RESERVATION_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "4025", "ReservationCoupon not found."),
    RESERVATION_COUPON_NOT_PREEMPTED(HttpStatus.NOT_FOUND, "4026", "선점된 쿠폰이 아닙니다"),
    COUPON_NOT_MATCHED_WITH_EVENT(HttpStatus.BAD_REQUEST, "4027", "이벤트의 쿠폰과 파라미터의 쿠폰이 같지 않습니다."),
    INVALID_EVENT_END_DATE(HttpStatus.BAD_REQUEST, "4028", "이벤트 종료 날짜가 적합하지 않습니다."),


    // event 조회 API Error
    INVALID_PARAMETERS_FOR_NON_ADMIN(HttpStatus.BAD_REQUEST, "4100", "User 권한을 가진 사용자가 요청 할 수 없는 파라미터 입니다."),

    // service common Error
    UNSUPPORTED_SORT_TYPE(HttpStatus.BAD_REQUEST, "4500", "지원하지 않는 정렬 방식입니다."),

    // 4900 ~ front 단에 같이 나가게 되는 Error
    CURRENT_EVENT_CLOSED(HttpStatus.CONFLICT, "4900", "이벤트가 이미 종료되었습니다."),
    CURRENT_EVENT_END_TO_OUT_OF_STOCK(HttpStatus.CONFLICT, "4910", "쿠폰 재고 마감으로 인해 이벤트가 종료되었습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
