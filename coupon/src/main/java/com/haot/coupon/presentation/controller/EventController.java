package com.haot.coupon.presentation.controller;

import com.haot.coupon.application.dto.request.events.EventSearchRequest;
import com.haot.coupon.application.dto.response.PageResponse;
import com.haot.coupon.application.dto.response.events.EventSearchResponse;
import com.haot.coupon.application.service.EventService;
import com.haot.coupon.common.response.ApiResponse;
import com.haot.coupon.common.response.enums.SuccessCode;
import com.haot.submodule.role.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController implements com.haot.coupon.presentation.docs.EventControllerDocs {

    private final EventService eventService;


    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{eventId}")
    public ApiResponse<EventSearchResponse> getEvent(@PathVariable(value = "eventId") String eventId) {
        return ApiResponse.success(SuccessCode.GET_DETAIL_EVENT_SUCCESS, eventService.getEvent(eventId));
    }


    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public ApiResponse<PageResponse<EventSearchResponse>> searchEvent(@RequestHeader("X-User-Role") Role userRole,
                                                                      @ModelAttribute EventSearchRequest request,
                                                                      Pageable pageable) {
        return ApiResponse.success(SuccessCode.SEARCH_EVENT_SUCCESS, eventService.searchEvent(userRole, request, pageable));
    }


}
