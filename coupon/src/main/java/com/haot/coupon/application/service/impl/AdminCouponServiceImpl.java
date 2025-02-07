package com.haot.coupon.application.service.impl;

import com.haot.coupon.application.dto.request.coupons.CouponCreateRequest;
import com.haot.coupon.application.dto.response.coupons.CouponCreateResponse;
import com.haot.coupon.application.mapper.CouponMapper;
import com.haot.coupon.application.service.AdminCouponService;
import com.haot.coupon.common.exceptions.CustomCouponException;
import com.haot.coupon.common.response.enums.ErrorCode;
import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.enums.CouponType;
import com.haot.coupon.domain.model.enums.DiscountPolicy;
import com.haot.coupon.infrastructure.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminCouponServiceImpl implements AdminCouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    @Transactional
    @Override
    public CouponCreateResponse create(CouponCreateRequest request) {

        // Business Logic TODO (아직 중복을 잡기 얘매한거 같다.)

        isCouponDurationLessThanOneDay(request.couponAvailableDate(), request.couponExpiredDate());

        DiscountPolicy discountPolicy = DiscountPolicy.checkDiscountPolicy(request.discountPolicy());
        CouponType couponType = CouponType.checkCouponType(request.couponType());

        // 할인율 쿠폰 -> discountRate, discountPolicy는 -> PERCENTAGE
        if (request.discountRate() != null) {
            validatePercentagePolicy(request, discountPolicy);
            // 금액 할인 쿠폰 -> discountAmount, discountPolicy는 -> AMOUNT
        } else {
            validateAmountPolicy(request, discountPolicy);
        }

        // 일단 선착순 쿠폰 1 ~ 100000개까지
        if (couponType == CouponType.PRIORITY) {
            if (request.totalQuantity() < 1 || request.totalQuantity() > 100000) {
                throw new CustomCouponException(ErrorCode.WRONG_TOTAL_QUANTITY);
            }
            // 무제한 쿠폰은 최대 발급 수량 -1로 받기
        } else {
            if (request.totalQuantity() != -1) {
                throw new CustomCouponException(ErrorCode.WRONG_TOTAL_QUANTITY);
            }
        }

        Coupon savedCoupon = couponRepository.save(couponMapper.toEntity(request));

        return couponMapper.responseId(savedCoupon.getId());
    }

    private void isCouponDurationLessThanOneDay(LocalDateTime availableDate, LocalDateTime expiredDate) {
        if (!availableDate.plusDays(1).isBefore(expiredDate)) {
            throw new CustomCouponException(ErrorCode.INSUFFICIENT_DATE_DIFFERENCE);
        }
    }

    // 할인율 쿠폰일때 유효성 검사
    private void validatePercentagePolicy(CouponCreateRequest request, DiscountPolicy discountPolicy) {
        if (request.discountAmount() != null) {
            throw new CustomCouponException(ErrorCode.TOO_MANY_DISCOUNT_POLICY);
        }
        if (discountPolicy != DiscountPolicy.PERCENTAGE) {
            throw new CustomCouponException(ErrorCode.DISCOUNT_POLICY_NOT_MATCH);
        }
    }

    // 금액 쿠폰일떼 우효성 검사
    private void validateAmountPolicy(CouponCreateRequest request, DiscountPolicy discountPolicy) {
        if (request.discountAmount() == null || request.discountAmount() <= 0) {
            throw new CustomCouponException(ErrorCode.WRONG_DISCOUNT_AMOUNT);
        }
        if (discountPolicy != DiscountPolicy.AMOUNT) {
            throw new CustomCouponException(ErrorCode.DISCOUNT_POLICY_NOT_MATCH);
        }
        // 쿠폰 최소 사용 금액 < discountAmount -> exception
        if (request.minDiscountAmount() < request.discountAmount()) {
            throw new CustomCouponException(ErrorCode.DISCOUNT_EXCEEDS_MIN_AMOUNT);
        }
        // 쿠폰 최대 사용 금액 < discountAmount -> exception
        if (request.maxDiscountAmount() < request.discountAmount()) {
            throw new CustomCouponException(ErrorCode.DISCOUNT_EXCEEDS_MAX_AMOUNT);
        }

    }
}
