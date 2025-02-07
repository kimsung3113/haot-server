package com.haot.coupon.presentation.controller;

import com.haot.coupon.application.dto.request.events.EventCreateRequest;
import com.haot.coupon.application.dto.request.events.EventModifyRequest;
import com.haot.coupon.application.dto.response.events.EventCreateResponse;
import com.haot.coupon.application.service.AdminEventService;
import com.haot.coupon.common.response.ApiResponse;
import com.haot.coupon.common.response.enums.SuccessCode;
import com.haot.coupon.presentation.docs.AdminEventControllerDocs;
import com.haot.submodule.role.Role;
import com.haot.submodule.role.RoleCheck;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/events")
public class AdminEventController implements AdminEventControllerDocs {

    private final AdminEventService adminEventService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @RoleCheck(Role.ADMIN)
    public ApiResponse<EventCreateResponse> create(@Valid @RequestBody EventCreateRequest eventCreateRequest) {
        return ApiResponse.SUCCESS(SuccessCode.CREATE_EVENT_SUCCESS, adminEventService.create(eventCreateRequest));
    }

    // 이벤트 수정 API
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{eventId}")
    @RoleCheck(Role.ADMIN)
    public ApiResponse<Void> modify(@RequestHeader(value = "X-User-Id", required = true) String userId,
                                    @PathVariable(value = "eventId") String eventId,
                                    @Valid @RequestBody EventModifyRequest eventModifyRequest) {

        adminEventService.modify(userId, eventId, eventModifyRequest);
        return ApiResponse.SUCCESS(SuccessCode.MODIFY_EVENT_SUCCESS);
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{eventId}")
    public ApiResponse<Void> delete(@PathVariable String eventId) {
        return ApiResponse.success();
    }



}
