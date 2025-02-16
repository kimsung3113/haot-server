package com.haot.coupon.utils;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.CouponEvent;
import com.haot.coupon.domain.model.enums.CouponType;
import com.haot.coupon.domain.model.enums.DiscountPolicy;
import com.haot.coupon.domain.model.enums.EventStatus;

import java.time.LocalDateTime;

public class TestEntityFixture {

    public static CouponEvent createMockCouponEvent(String couponEventId, CouponType couponType){

        CouponEvent couponEvent = mock(CouponEvent.class);
        lenient().when(couponEvent.getId()).thenReturn(couponEventId);
        Coupon coupon = createMockCoupon(couponType);
        lenient().when(couponEvent.getCoupon()).thenReturn(coupon);
        lenient().when(couponEvent.getEventStartDate()).thenReturn(LocalDateTime.now());
        lenient().when(couponEvent.getEventEndDate()).thenReturn(LocalDateTime.now().plusDays(1));
        lenient().when(couponEvent.getEventName()).thenReturn("TestEvent");
        lenient().when(couponEvent.getDescription()).thenReturn("TestEvent Description");
        lenient().when(couponEvent.getEventStatus()).thenReturn(EventStatus.DEFAULT);

        return couponEvent;
    }

    public static Coupon createMockCoupon(CouponType couponType){
        Coupon coupon = mock(Coupon.class);
        lenient().when(coupon.getId()).thenReturn("TestCouponId");
        lenient().when(coupon.getName()).thenReturn("TestCoupon");
        lenient().when(coupon.getDiscountAmount()).thenReturn(5000.0);
        lenient().when(coupon.getIssuedQuantity()).thenReturn(0);
        lenient().when(coupon.getDiscountPolicy()).thenReturn(DiscountPolicy.AMOUNT);
        lenient().when(coupon.getMinAvailableAmount()).thenReturn(30000.0);
        lenient().when(coupon.getMaxAvailableAmount()).thenReturn(300000.0);
        lenient().when(coupon.getAvailableDate()).thenReturn(LocalDateTime.now());
        lenient().when(coupon.getExpiredDate()).thenReturn(LocalDateTime.now().plusDays(15));
        lenient().when(coupon.getType()).thenReturn(couponType);

        if(couponType == CouponType.PRIORITY){
            lenient().when(coupon.getTotalQuantity()).thenReturn(100);
        }else{
            lenient().when(coupon.getTotalQuantity()).thenReturn(-1);
        }
        return coupon;
    }

}
