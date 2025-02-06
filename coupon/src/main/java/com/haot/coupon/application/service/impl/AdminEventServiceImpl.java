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
import com.haot.coupon.domain.model.enums.CouponType;
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

        // Business Logic

        // eventEndDate이 eventStartDate보다 +1일 보다 후여야 한다.
        if (!eventCreateRequest.eventStartDate().plusDays(1).isBefore(eventCreateRequest.eventEndDate())) {
            throw new CustomCouponException(ErrorCode.INSUFFICIENT_DATE_DIFFERENCE);
        }

        // event 끝 날짜가 쿠폰 만료 날짜보다 전이어야 된다.
        Coupon coupon = couponRepository.findByIdAndExpiredDateIsAfterAndIsDeletedFalse(eventCreateRequest.couponId(),
                        eventCreateRequest.eventEndDate())
                .orElseThrow(() -> new CustomCouponException(ErrorCode.COUPON_NOT_FOUND));


        // 이벤트에 같은 선착순 쿠폰이 설정되면 안된다, 무제한은 가능하게!
        if (CouponType.PRIORITY == coupon.getType() && couponEventRepository.existsByCouponIdAndIsDeletedFalse(coupon.getId())) {
            throw new CustomCouponException(ErrorCode.EXIST_PRIORITY_COUPON_EVENTS);
        }

        // 무제한은 이벤트들 안에서 여러번 같은 쿠폰을 쓸 수 있게 한다.
        if (CouponType.UNLIMITED == coupon.getType()) {

            List<CouponEvent> promotions = couponEventRepository
                    .findByCouponIdAndEventEndDateIsAfterAndIsDeletedFalse(coupon.getId(), LocalDateTime.now())
                    .stream()
                    .peek(couponEvent -> {
                        if (couponEvent.getEventStatus() == EventStatus.DEFAULT) {
                            throw new CustomCouponException(ErrorCode.EXIST_UNLIMITED_COUPON_EVENTS);
                        }
                    })
                    .toList();

        }

        // mapper로 event 만든다.
        CouponEvent event = eventMapper.toEntity(eventCreateRequest, coupon);

        // 현재 시간으로 eventStatus update
        event.updateExpiredEventStatus();

        CouponEvent savedEvent = couponEventRepository.save(event);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (coupon.checkPriorityCoupon()) {
                    log.info("선착순 쿠폰 : {} Redis 적용", coupon.getId());
                    redisRepository.save(savedEvent, coupon);
                }
            }
        });

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
