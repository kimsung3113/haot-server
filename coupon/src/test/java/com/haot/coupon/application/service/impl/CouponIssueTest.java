package com.haot.coupon.application.service.impl;

import com.haot.coupon.application.cache.RedisRepository;
import com.haot.coupon.application.dto.request.coupons.CouponCustomerCreateRequest;
import com.haot.coupon.application.kafka.CouponErrorProducer;
import com.haot.coupon.application.kafka.CouponIssueProducer;
import com.haot.coupon.application.mapper.CouponMapper;
import com.haot.coupon.application.mapper.EventMapper;
import com.haot.coupon.common.exceptions.CustomCouponException;
import com.haot.coupon.common.response.enums.ErrorCode;
import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.CouponEvent;
import com.haot.coupon.domain.model.enums.CouponType;
import com.haot.coupon.domain.utils.CouponIssueRedisCode;
import com.haot.coupon.infrastructure.repository.CouponEventRepository;
import com.haot.coupon.utils.TestDtoFixture;
import com.haot.coupon.utils.TestEntityFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponIssueTest {

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
        CouponCustomerCreateRequest request = TestDtoFixture.createCouponCustomerCreateRequest();

        CouponEvent event = TestEntityFixture.createMockCouponEvent("testEventId", CouponType.PRIORITY);
        Coupon coupon = event.getCoupon();

        when(coupon.checkPriorityCoupon()).thenReturn(true); // 우선순위 쿠폰 발급 시나리오

        when(couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(eq(event.getId()), any()))
                .thenReturn(Optional.of(event));

        // TODO 왜 2번째 매개변수인 couponid만 eq를 했을때 에러가 나는 이유는..?
        // Redis Mock: LuaScript 호출 결과 설정
        when(redisRepository.issuePriorityCoupon(eq(event.getId()), anyString(), eq(userId), any()))
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

        when(couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(eq(event.getId()), any()))
                .thenReturn(Optional.of(event));

        // Redis Mock: LuaScript 호출 결과 설정
        when(redisRepository.issueUnlimitedCoupon(eq(coupon.getId()), eq(userId), any()))
                .thenReturn(CouponIssueRedisCode.SUCCESS);

        // Kafka 메시지 전송은 void 메서드이므로 doNothing() 사용
        doNothing().when(couponIssueProducer).sendIssueUnlimitedCoupon(any());

        // When
        couponService.customerIssueCoupon(request, userId);

        // Then
        verify(couponIssueProducer).sendIssueUnlimitedCoupon(any()); // Kafka 메시지 발행 검증
    }

    @Test
    @DisplayName("이벤트가 없을 시 coupon 발급 error")
    void testCustomerIssueCoupon_ErrorEventNotFound() {
        // Given
        String userId = "testUser";
        CouponCustomerCreateRequest request = TestDtoFixture.createCouponCustomerCreateRequest();

        when(couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(anyString(), any()))
                .thenReturn(Optional.empty());

        // When & Then
        CustomCouponException exception = assertThrows(CustomCouponException.class,
                () -> couponService.customerIssueCoupon(request, userId));

        assertEquals(ErrorCode.EVENT_NOT_FOUND, exception.getResCode());
    }

    @Test
    @DisplayName("event Expired -> coupon 발급 error")
    void testCustomerIssueCoupon_ErrorEventClosedExpired() {
        // Given
        String userId = "testUser";
        CouponCustomerCreateRequest request = TestDtoFixture.createCouponCustomerCreateRequest();

        CouponEvent event = TestEntityFixture.createMockExpiredCouponEvent();

        when(couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(eq(event.getId()), any()))
                .thenReturn(Optional.of(event));

        doNothing().when(couponErrorProducer).sendEventClosed(any());

        // When
        CustomCouponException exception = assertThrows(CustomCouponException.class,
                () -> couponService.customerIssueCoupon(request, userId));

        // Then
        assertEquals(ErrorCode.CURRENT_EVENT_CLOSED, exception.getResCode());
        verify(couponErrorProducer).sendEventClosed(any());
    }

    @Test
    @DisplayName("event Out of Stock -> coupon 발급 error")
    void testCustomerIssuePriorityCoupon_ErrorEventClosedOutOfStock() {
        // Given
        String userId = "testUser";
        CouponCustomerCreateRequest request = TestDtoFixture.createCouponCustomerCreateRequest();

        CouponEvent event = TestEntityFixture.createMockCouponEvent("testEventId", CouponType.PRIORITY);
        Coupon coupon = event.getCoupon();

        when(coupon.checkPriorityCoupon()).thenReturn(true);

        when(couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(eq(event.getId()), any()))
                .thenReturn(Optional.of(event));

        when(redisRepository.issuePriorityCoupon(eq(event.getId()), anyString(), eq(userId), any()))
                .thenReturn(CouponIssueRedisCode.EXCEEDED_LIMIT);

        doNothing().when(couponErrorProducer).sendEventClosed(any());

        // When
        CustomCouponException exception = assertThrows(CustomCouponException.class,
                () -> couponService.customerIssueCoupon(request, userId));

        // Then
        assertEquals(ErrorCode.CURRENT_EVENT_END_TO_OUT_OF_STOCK, exception.getResCode());
        verify(couponErrorProducer).sendEventClosed(any());
    }

    @Test
    @DisplayName("이미 쿠폰 발급 받은 user -> coupon 발급 error")
    void testCustomerIssuePriorityCoupon_ErrorAlreadyIssued() {
        // Given
        String userId = "testUser";
        CouponCustomerCreateRequest request = TestDtoFixture.createCouponCustomerCreateRequest();

        CouponEvent event = TestEntityFixture.createMockCouponEvent("testEventId", CouponType.PRIORITY);
        Coupon coupon = event.getCoupon();

        when(coupon.checkPriorityCoupon()).thenReturn(true);
        when(couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(eq(event.getId()), any()))
                .thenReturn(Optional.of(event));

        when(redisRepository.issuePriorityCoupon(eq(event.getId()), anyString(), eq(userId), any()))
                .thenThrow(new CustomCouponException(ErrorCode.DUPLICATED_ISSUED_COUPON));;

        // When
        CustomCouponException exception = assertThrows(CustomCouponException.class,
                () -> couponService.customerIssueCoupon(request, userId));

        // Then
        assertEquals(ErrorCode.DUPLICATED_ISSUED_COUPON, exception.getResCode());
    }

}