package com.haot.coupon.application.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.haot.coupon.application.cache.RedisRepository;
import com.haot.coupon.application.dto.request.coupons.CouponCustomerCreateRequest;
import com.haot.coupon.application.kafka.CouponErrorProducer;
import com.haot.coupon.application.kafka.CouponIssueProducer;
import com.haot.coupon.application.mapper.CouponMapper;
import com.haot.coupon.application.mapper.EventMapper;
import com.haot.coupon.application.service.CouponService;
import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.CouponEvent;
import com.haot.coupon.domain.model.enums.CouponType;
import com.haot.coupon.domain.utils.CouponIssueRedisCode;
import com.haot.coupon.infrastructure.repository.CouponEventRepository;
import com.haot.coupon.utils.TestDtoFixture;
import com.haot.coupon.utils.TestEntityFixture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @InjectMocks
    private CouponServiceImpl couponService;

    @Mock
    private CouponEventRepository couponEventRepository;

    @Mock
    private RedisRepository redisRepository; // LuaScript Mocking

    @Mock
    private CouponIssueProducer couponIssueProducer; // Kafka 메시지 Mocking

    @Mock
    private CouponErrorProducer couponErrorProducer;

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private EventMapper eventMapper;

    @Test
    @DisplayName("선착순 쿠폰 발급 성공")
    void testCustomerIssuePriorityCoupon_Success() {
        // Given
        String userId = "testUser";
        CouponCustomerCreateRequest request = new CouponCustomerCreateRequest("testEventId", "TestCouponId");

        CouponEvent event = TestEntityFixture.createMockCouponEvent("testEventId", CouponType.PRIORITY);
        Coupon coupon = event.getCoupon();

        when(coupon.checkPriorityCoupon()).thenReturn(true); // 우선순위 쿠폰 발급 시나리오

        when(couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(anyString(), any()))
                .thenReturn(Optional.of(event));

        // Redis Mock: LuaScript 호출 결과 설정
        when(redisRepository.issuePriorityCoupon(anyString(), anyString(), anyString(), any()))
                .thenReturn(CouponIssueRedisCode.SUCCESS);

        // Kafka 메시지 전송은 void 메서드이므로 doNothing() 사용
        doNothing().when(couponIssueProducer).sendIssuePriorityCoupon(any());

        // When
        couponService.customerIssueCoupon(request, userId);

        // Then
        verify(couponIssueProducer).sendIssuePriorityCoupon(any()); // Kafka 메시지 발행 검증
    }

    @Test
    @DisplayName("무제한 쿠폰 발급 성공")
    void testCustomerIssueUnlimitedCoupon_Success() {
        // Given
        String userId = "testUser";
        CouponCustomerCreateRequest request = TestDtoFixture.createCouponCustomerCreateRequest();

        CouponEvent event = TestEntityFixture.createMockCouponEvent("testEventId", CouponType.UNLIMITED);
        Coupon coupon = event.getCoupon();

        when(coupon.checkPriorityCoupon()).thenReturn(false);

        when(couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(anyString(), any()))
                .thenReturn(Optional.of(event));

        // Redis Mock: LuaScript 호출 결과 설정
        when(redisRepository.issueUnlimitedCoupon(anyString(), anyString(), any()))
                .thenReturn(CouponIssueRedisCode.SUCCESS);

        // Kafka 메시지 전송은 void 메서드이므로 doNothing() 사용
        doNothing().when(couponIssueProducer).sendIssueUnlimitedCoupon(any());

        // When
        couponService.customerIssueCoupon(request, userId);

        // Then
        verify(couponIssueProducer).sendIssueUnlimitedCoupon(any()); // Kafka 메시지 발행 검증
    }



}