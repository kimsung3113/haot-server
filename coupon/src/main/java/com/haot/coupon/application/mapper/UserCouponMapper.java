package com.haot.coupon.application.mapper;

import com.haot.coupon.application.dto.response.coupons.CouponReadMeResponse;
import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.UserCoupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserCouponMapper {

    @Mapping(target = "coupon.id", source = "coupon.id")
    @Mapping(target = "couponStatus", constant = "DISTRIBUTED")
    @Mapping(target = "usedDate", ignore = true)
    UserCoupon toEntity(String userId, Coupon coupon);

    @Mapping(target = "coupon.id", source = "couponId")
    @Mapping(target = "couponStatus", constant = "DISTRIBUTED")
    @Mapping(target = "usedDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    UserCoupon toEntity(String userId, String couponId);

    // QueryDsl mapping
    @Mapping(source = "coupon.id", target = "couponId")
    @Mapping(source = "coupon.name", target = "couponName")
    @Mapping(source = "coupon.availableDate", target = "couponAvailableDate")
    @Mapping(source = "coupon.expiredDate", target = "couponExpiredDate")
    @Mapping(source = "coupon.minAvailableAmount", target = "minAvailableAmount")
    @Mapping(source = "coupon.maxAvailableAmount", target = "maxAvailableAMount")
    @Mapping(source = "coupon.discountRate.rate", target = "discountRate")
    @Mapping(source = "coupon.discountAmount", target = "discountAmount")
    CouponReadMeResponse toCouponReadMeResponse(UserCoupon userCoupon);


}
