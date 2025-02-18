package com.haot.coupon.infrastructure.kafka;

import com.haot.coupon.application.dto.EventClosedDto;
import com.haot.coupon.application.kafka.CouponErrorConsumer;
import com.haot.coupon.application.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j(topic = "CouponErrorFrontListener")
@RequiredArgsConstructor
public class CouponErrorFrontConsumer implements CouponErrorConsumer {

    private final CouponService couponService;

    @KafkaListener(topics = "coupon-event-end", groupId = "coupon-event-error",
            containerFactory = "kafkaListenerContainerFactory")
    public void eventErrorListener(List<EventClosedDto> eventClosedDtoList, Acknowledgment acknowledgment) {

        Set<EventClosedDto> uniqueEvents = new HashSet<>(eventClosedDtoList);

        couponService.updateEndEventStatus(uniqueEvents);
        acknowledgment.acknowledge();
    }

}
