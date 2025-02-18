package com.haot.coupon.utils;

import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.CouponEvent;
import com.haot.coupon.domain.model.enums.CouponType;
import com.haot.coupon.domain.model.enums.DiscountPolicy;
import com.haot.coupon.domain.model.enums.EventStatus;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class TestEntityFixture {

    public static CouponEvent createCouponEvent(Coupon coupon){
        return CouponEvent.builder()
                .coupon(coupon)
                .eventName("Redis Concurrency EventTest")
                .description("Redis Concurrency EventTest Description")
                .eventStartDate(LocalDateTime.now())
                .eventEndDate(LocalDateTime.now().plusDays(1))
                .eventStatus(EventStatus.DEFAULT)
                .build();
    }



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

    public static Coupon createPriorityCoupon(){
        return Coupon.builder()
                .name("Redis Concurrency testCoupon")
                .discountAmount(5000.0)
                .issuedQuantity(0)
                .totalQuantity(3)
                .discountPolicy(DiscountPolicy.AMOUNT)
                .minAvailableAmount(30000.0)
                .maxAvailableAmount(300000.0)
                .availableDate(LocalDateTime.now())
                .expiredDate(LocalDateTime.now().plusDays(15))
                .type(CouponType.PRIORITY)
                .build();
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

    public static CouponEvent createMockExpiredCouponEvent(){
        CouponEvent couponEvent = mock(CouponEvent.class);
        Coupon coupon = createMockCoupon(CouponType.PRIORITY);
        lenient().when(couponEvent.getCoupon()).thenReturn(coupon);
        lenient().when(couponEvent.getEventStartDate()).thenReturn(LocalDateTime.now().minusDays(10));
        lenient().when(couponEvent.getEventEndDate()).thenReturn(LocalDateTime.now().minusMinutes(1));
        return couponEvent;
    }

}
