package com.haot.coupon.presentation.controller;

import com.haot.coupon.application.dto.feign.request.FeignConfirmReservationRequest;
import com.haot.coupon.application.dto.feign.request.FeignVerifyRequest;
import com.haot.coupon.application.dto.request.coupons.CouponCustomerCreateRequest;
import com.haot.coupon.application.dto.response.coupons.CouponReadMeResponse;
import com.haot.coupon.application.dto.response.coupons.CouponSearchResponse;
import com.haot.coupon.application.dto.feign.response.ReservationVerifyResponse;
import com.haot.coupon.application.service.CouponService;
import com.haot.coupon.common.response.ApiResponse;
import com.haot.coupon.common.response.enums.SuccessCode;
import com.haot.coupon.presentation.docs.CouponControllerDocs;
import com.haot.submodule.role.Role;
import com.haot.submodule.role.RoleCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController implements CouponControllerDocs {

    private final CouponService couponService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    @RoleCheck({Role.ADMIN, Role.USER})
    public ApiResponse<Page<CouponReadMeResponse>> getMyCoupons(@RequestHeader("X-User-Id") String userId,
                                                                Pageable pageable) {
        return ApiResponse.success(SuccessCode.GET_USER_COUPONS_SUCCESS, couponService.getMyCoupons(userId, pageable));
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{couponId}")
    @RoleCheck({Role.ADMIN, Role.USER})
    public ApiResponse<CouponSearchResponse> getCouponDetails(@PathVariable(value = "couponId") String couponId) {
        return ApiResponse.success(SuccessCode.GET_DETAIL_COUPON_SUCCESS, couponService.getCouponDetails(couponId));
    }

    // TODO userId 다시 변경하기
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/issued")
    @RoleCheck({Role.ADMIN, Role.USER})
    public ApiResponse<Void> customerIssueCoupon(// @RequestHeader("X-User-Id") String userId,
                                                 @RequestBody CouponCustomerCreateRequest request) {

        String userId = UUID.randomUUID().toString();

        couponService.customerIssueCoupon(request, userId);

        return ApiResponse.success(SuccessCode.CUSTOMER_ISSUED_COUPON_SUCCESS);
    }

    // 유효성 검사 API
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/verify")
    public ApiResponse<ReservationVerifyResponse> verify(@RequestHeader("X-User-Id") String userId,
                                                         @RequestBody FeignVerifyRequest request) {
        return ApiResponse.success(SuccessCode.VERIFY_COUPON_SUCCESS, couponService.verify(userId, request));
    }

    // 쿠폰 Rollback API
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{reservationCouponId}/rollback")
    public ApiResponse<Void> rollbackReservationCoupon(@RequestHeader("X-User-Id") String userId,
                                                       @RequestHeader("X-User-Role") Role role,
                                                       @PathVariable(value = "reservationCouponId") String reservationCouponId) {

        couponService.rollbackReservationCoupon(userId, role, reservationCouponId);
        return ApiResponse.success(SuccessCode.COUPON_RESERVATION_ROLLBACK_SUCCESS);
    }

    // [Feign] 예약 취소 or 확정 API
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{reservationCouponId}")
    public ApiResponse<Void> confirmReservation(@RequestHeader("X-User-Id") String userId,
                                                @PathVariable(value = "reservationCouponId") String reservationCouponId,
                                                @RequestBody FeignConfirmReservationRequest request) {

        couponService.confirmReservation(reservationCouponId, request);
        return ApiResponse.success(SuccessCode.COUPON_RESERVATION_SUCCESS);
    }


}
