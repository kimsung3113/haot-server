package com.haot.coupon.presentation.docs;

import com.haot.coupon.application.dto.feign.request.FeignConfirmReservationRequest;
import com.haot.coupon.application.dto.feign.request.FeignVerifyRequest;
import com.haot.coupon.application.dto.feign.response.ReservationVerifyResponse;
import com.haot.coupon.application.dto.request.coupons.CouponCustomerCreateRequest;
import com.haot.coupon.application.dto.response.coupons.CouponReadMeResponse;
import com.haot.coupon.application.dto.response.coupons.CouponSearchResponse;
import com.haot.coupon.common.response.ApiResponse;
import com.haot.submodule.role.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "쿠폰 API Controller", description = "쿠폰 API 목록입니다.")
public interface CouponControllerDocs {

    @Operation(summary = "내 쿠폰함 보기 API", description = "쿠폰함 보기 API 입니다. 관리자는 파라미터 추가 가능")
    ApiResponse<Page<CouponReadMeResponse>> getMyCoupons(String userId, Pageable pageable);

    @Operation(summary = "쿠폰 단건 조회 API", description = "쿠폰 단 건 조회 API 입니다.")
    ApiResponse<CouponSearchResponse> getCouponDetails(String couponId);

    @Operation(summary = "쿠폰 발급 API", description = "쿠폰 발급 API 입니다.")
    ApiResponse<Void> customerIssueCoupon(/*String userId, */CouponCustomerCreateRequest request);

    @Operation(summary = "쿠폰 유효성 검사 API", description = "[Feign] 쿠폰 사용하기 전 유효성 검사 API 입니다.")
    ApiResponse<ReservationVerifyResponse> verify(String userId, FeignVerifyRequest request);

    @Operation(summary = "쿠폰 rollback API", description = "[Feign] 쿠폰 사용 취소시 쿠폰 사용 Rollback API 입니다.")
    ApiResponse<Void> rollbackReservationCoupon(String userId, Role role, String reservationCouponId);

    @Operation(summary = "쿠폰 사용 확정 API", description = "[Feign] 쿠폰 사용 확정하는 API 입니다.")
    ApiResponse<Void> confirmReservation(String userId, String reservationCouponId, FeignConfirmReservationRequest request);
}
