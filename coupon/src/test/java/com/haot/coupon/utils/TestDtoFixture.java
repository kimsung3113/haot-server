package com.haot.coupon.utils;

import com.haot.coupon.application.dto.request.coupons.CouponCustomerCreateRequest;

public class TestDtoFixture {

    public static CouponCustomerCreateRequest createCouponCustomerCreateRequest() {
        return CouponCustomerCreateRequest.builder()
                .eventId("testEventId")
                .couponId("TestCouponId")
                .build();
    }

}
