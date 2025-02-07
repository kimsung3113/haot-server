package com.haot.coupon.application.service.impl;

import com.haot.coupon.application.cache.RedisRepository;
import com.haot.coupon.application.dto.request.events.EventCreateRequest;
import com.haot.coupon.application.dto.request.events.EventModifyRequest;
import com.haot.coupon.application.dto.response.events.EventCreateResponse;
import com.haot.coupon.application.kafka.CouponErrorProducer;
import com.haot.coupon.application.mapper.EventMapper;
import com.haot.coupon.application.service.AdminEventService;
import com.haot.coupon.common.exceptions.CustomCouponException;
import com.haot.coupon.common.response.enums.ErrorCode;
import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.CouponEvent;
import com.haot.coupon.domain.model.enums.EventStatus;
import com.haot.coupon.infrastructure.repository.CouponEventRepository;
import com.haot.coupon.infrastructure.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Slf4j(topic = "AdminEventServiceImpl")
@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final CouponEventRepository couponEventRepository;
    private final CouponRepository couponRepository;

    private final EventMapper eventMapper;

    private final RedisRepository redisRepository;

    private final CouponErrorProducer couponErrorProducer;

    @Transactional
    @CacheEvict(value = "eventPageCache", allEntries = true)
    @Override
    public EventCreateResponse create(EventCreateRequest eventCreateRequest) {

        isEventDurationLessThanOneDay(eventCreateRequest.eventStartDate(), eventCreateRequest.eventEndDate());
        Coupon coupon = validateCoupon(eventCreateRequest);

        validateEventWithCoupon(coupon);
        CouponEvent savedEvent = saveEventWithTransactionHandling(eventCreateRequest, coupon);

        return eventMapper.toCreateResponse(savedEvent);
    }

    // 이벤트 수정 API, 문제가 생겼을때 확산 방지용 API or 이름, 설명 변경 API
    @Transactional
    @CacheEvict(value = "eventPageCache", allEntries = true,
            condition = "#request.eventStatus() != null")
    @Override
    public void modify(String userId, String eventId, EventModifyRequest request) {

        validateNonEmptyFields(request.eventName(), request.eventDescription(), request.eventStatus());

        CouponEvent event = couponEventRepository.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new CustomCouponException(ErrorCode.EVENT_NOT_FOUND));

        // status 수정시 이벤트 관리자 강제 종료
        if (request.eventStatus() != null) {

            // 이미 끝난 이벤트는 status 수정 필요가 없게
            if (event.getEventStatus() != EventStatus.DEFAULT) {
                throw new CustomCouponException(ErrorCode.CURRENT_EVENT_CLOSED);
            }

            // 이벤트가 시작하기 전이면 상태값만 변경 -> 할당된 쿠폰은 쓰지 못하고 확인 후 delete 해야될듯
            if (LocalDateTime.now().isBefore(event.getEventStartDate())) {
                event.updateEventStatus(EventStatus.MANUALLY_CLOSED);
            } else {
                Coupon coupon = event.getCoupon();
                deleteCoupon(coupon, userId);
                couponErrorProducer.sendEventClosed(eventMapper.toProduce(eventId, EventStatus.MANUALLY_CLOSED));
            }

        } else {
            // 이름, description 수정
            event.modifyEvent(request.eventName(), request.eventDescription());
        }
    }

    private void isEventDurationLessThanOneDay(LocalDateTime startDate, LocalDateTime endDate) {
        if (!startDate.plusDays(1).isBefore(endDate)) {
            throw new CustomCouponException(ErrorCode.INSUFFICIENT_DATE_DIFFERENCE);
        }
    }

    private Coupon validateCoupon(EventCreateRequest request) {
        Coupon coupon = couponRepository.findByIdAndIsDeletedFalse(request.couponId())
                .orElseThrow(() -> new CustomCouponException(ErrorCode.COUPON_NOT_FOUND));

        if (!request.eventEndDate().isBefore(coupon.getExpiredDate())) {
            throw new CustomCouponException(ErrorCode.INVALID_EVENT_END_DATE);
        }

        return coupon;
    }

    private void validateEventWithCoupon(Coupon coupon) {
        if (coupon.checkPriorityCoupon()) {
            if(existsEventPriorityCoupon(coupon.getId())) {
                throw new CustomCouponException(ErrorCode.EXIST_PRIORITY_COUPON_EVENTS);
            }
        }else{
            List<CouponEvent> promotions =
            couponEventRepository.findByCouponIdAndEventStatusAndIsDeletedFalse(coupon.getId(), EventStatus.DEFAULT);

            if(!promotions.isEmpty()){
                throw new CustomCouponException(ErrorCode.EXIST_UNLIMITED_COUPON_EVENTS);
            }
        }
    }

    private boolean existsEventPriorityCoupon(String couponId) {
        return couponEventRepository.existsByCouponIdAndIsDeletedFalse(couponId);
    }


    private CouponEvent saveEventWithTransactionHandling(EventCreateRequest request, Coupon coupon) {
        CouponEvent savedEvent = couponEventRepository.save(eventMapper.toEntity(request, coupon));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                handlePostCommitRedisSave(coupon, savedEvent);
            }
        });

        return savedEvent;
    }


    private void handlePostCommitRedisSave(Coupon coupon, CouponEvent savedEvent) {
        if (coupon.checkPriorityCoupon()) {
            log.info("선착순 쿠폰 이벤트: {} Redis 저장", coupon.getId());
            redisRepository.save(savedEvent, coupon);
        }
    }


    // 쿠폰 삭제
    private void deleteCoupon(Coupon coupon, String userId) {
        coupon.deleteEntity(userId);
    }

    // 유효성 검사 함수: 필드들이 모두 비어 있을 경우 예외 던짐
    private void validateNonEmptyFields(String eventName, String eventDescription, String eventStatus) {
        boolean allFieldsEmpty = Stream.of(eventName, eventDescription, eventStatus)
                .allMatch(field -> field == null || field.isEmpty());

        if (allFieldsEmpty) {
            throw new CustomCouponException(ErrorCode.MODIFY_EVENT_HAS_NO_PARAMETER);
        }
    }
}
