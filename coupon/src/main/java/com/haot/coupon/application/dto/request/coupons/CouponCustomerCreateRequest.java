package com.haot.coupon.application.dto.request.coupons;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "쿠폰 발급 REQUEST DTO")
@Builder
public record CouponCustomerCreateRequest(
        String eventId,
        String couponId
){
}
