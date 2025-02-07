package com.haot.coupon.application.service.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.haot.coupon.application.cache.RedisRepository;
import com.haot.coupon.application.dto.CheckAlreadyClosedEventDto;
import com.haot.coupon.application.dto.CouponIssueDto;
import com.haot.coupon.application.dto.EventClosedDto;
import com.haot.coupon.application.dto.feign.request.FeignConfirmReservationRequest;
import com.haot.coupon.application.dto.feign.request.FeignVerifyRequest;
import com.haot.coupon.application.dto.feign.response.ReservationVerifyResponse;
import com.haot.coupon.application.dto.request.coupons.CouponCustomerCreateRequest;
import com.haot.coupon.application.dto.response.coupons.CouponReadMeResponse;
import com.haot.coupon.application.dto.response.coupons.CouponSearchResponse;
import com.haot.coupon.application.kafka.CouponErrorProducer;
import com.haot.coupon.application.kafka.CouponIssueProducer;
import com.haot.coupon.application.mapper.CouponMapper;
import com.haot.coupon.application.mapper.EventMapper;
import com.haot.coupon.application.mapper.ReservationCouponMapper;
import com.haot.coupon.application.mapper.UserCouponMapper;
import com.haot.coupon.application.service.CouponService;
import com.haot.coupon.common.exceptions.CustomCouponException;
import com.haot.coupon.common.response.enums.ErrorCode;
import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.CouponEvent;
import com.haot.coupon.domain.model.ReservationCoupon;
import com.haot.coupon.domain.model.UserCoupon;
import com.haot.coupon.domain.model.enums.EventStatus;
import com.haot.coupon.domain.model.enums.ReservationCouponStatus;
import com.haot.coupon.domain.utils.CouponIssueRedisCode;
import com.haot.coupon.infrastructure.repository.CouponEventRepository;
import com.haot.coupon.infrastructure.repository.CouponRepository;
import com.haot.coupon.infrastructure.repository.ReservationCouponRepository;
import com.haot.coupon.infrastructure.repository.UserCouponRepository;
import com.haot.submodule.role.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.*;

@Slf4j(topic = "CouponServiceImpl")
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponEventRepository couponEventRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final ReservationCouponRepository reservationCouponRepository;

    private final RedisRepository redisRepository;

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final ReservationCouponMapper reservationCouponMapper;
    private final EventMapper eventMapper;

    private final CouponErrorProducer couponErrorProducer;
    private final CouponIssueProducer couponIssueProducer;

    // 쿠폰 발급 API
    @Transactional
    @Override
    public void customerIssueCoupon(CouponCustomerCreateRequest request, String userId) {

        CouponEvent event = couponEventRepository.findByIdAndEventStatusAndIsDeleteFalse(request.eventId(), EventStatus.DEFAULT)
                .orElseThrow(() -> new CustomCouponException(ErrorCode.EVENT_NOT_FOUND));

        Coupon coupon = event.getCoupon();

        if (!coupon.getId().equals(request.couponId())) {
            throw new CustomCouponException(ErrorCode.COUPON_NOT_MATCHED_WITH_EVENT);
        }

        checkEventDate(event, LocalDateTime.now());

        if (coupon.checkPriorityCoupon()) {
            checkPriorityCouponStock(event.getId(), coupon.getId(), userId, event.getEventEndDate());
            couponIssueProducer.sendIssuePriorityCoupon(couponMapper.toCouponIssueDto(userId, request)); // TODO auditoraware 사용해 updateby에 잘들어가게 해야된다 & dto로 바꿔야됨.
        } else {
            CouponIssueRedisCode code = redisRepository.issueUnlimitedCoupon(coupon.getId(), userId, event.getEventEndDate());
            couponIssueProducer.sendIssueUnlimitedCoupon(couponMapper.toCouponIssueDto(userId, request));
        }

    }

    // 쿠폰 단건 조회 API
    @Transactional(readOnly = true)
    @Override
    public CouponSearchResponse getCouponDetails(String couponId) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomCouponException(ErrorCode.COUPON_NOT_FOUND));

        return couponMapper.toSearchResponse(coupon);
    }

    // 쿠폰 유효성 검사 API
    @Transactional
    @Override
    public ReservationVerifyResponse verify(String userId, FeignVerifyRequest request) {

        Coupon coupon = checkExistsCoupon(request.couponId());
        UserCoupon userCoupon = findUserCoupon(userId, coupon);

        checkReservedCouponAvailable(userCoupon);
        checkIfCouponUsed(userCoupon);
        validateBeforeReservation(coupon, request);

        double totalPrice = request.reservationPrice();
        double discountPrice = getDiscountedPrice(coupon, totalPrice);

        ReservationCoupon reservationCoupon = reservationCouponRepository.save(
                reservationCouponMapper.toEntity(userCoupon, totalPrice, discountPrice)
        );

        return reservationCouponMapper.toVerifyFeignResponse(reservationCoupon.getId(), totalPrice - discountPrice);
    }

    // [Feign] 쿠폰 상태 변경 API
    @Transactional
    @Override
    public void confirmReservation(String reservationCouponId, FeignConfirmReservationRequest request) {
        ReservationCoupon reservationCoupon = reservationCouponRepository.findById(reservationCouponId)
                .orElseThrow(() -> new CustomCouponException(ErrorCode.RESERVATION_COUPON_NOT_FOUND));

        // 선점 상태가 아닌 경우 에러 반환
        validateReservationPreemption(reservationCoupon);

        ReservationCouponStatus reservationCouponStatus = ReservationCouponStatus.checkReservationCouponStatus(request.reservationStatus());

        // 예약 상태 처리
        handleReservation(reservationCoupon, reservationCouponStatus);
    }

    // 내 쿠폰함 보기 API
    @Transactional(readOnly = true)
    @Override
    public Page<CouponReadMeResponse> getMyCoupons(String userId, Pageable pageable) {
        return userCouponRepository.checkMyCouponBox(userId, pageable);
    }

    // 쿠폰 Rollback API
    @Transactional
    @Override
    public void rollbackReservationCoupon(String userId, Role role, String reservationCouponId) {

        ReservationCoupon reservationCoupon = reservationCouponRepository.findById(reservationCouponId)
                .orElseThrow(() -> new CustomCouponException(ErrorCode.RESERVATION_COUPON_NOT_FOUND));

        UserCoupon userCoupon = reservationCoupon.getUserCoupon();


        validateUserAndRole(userId, role, userCoupon.getUserId());
        validateReservationPreemption(reservationCoupon);

        reservationCoupon.confirmReservationStatus(ReservationCouponStatus.ROLLBACK);
    }

    @Transactional
    @Override
    public void batchIssueCoupon(List<CouponIssueDto> requests) {

        Map<String, Set<String>> userIdsByCouponId = requests.stream()
                .collect
                        (groupingBy(CouponIssueDto::getCouponId, mapping(CouponIssueDto::getUserId, toSet())));

        userIdsByCouponId.forEach((couponId, value) -> {

            StreamSupport.stream(Iterables.partition(value, 100).spliterator(), false)
                    .forEach(userIds -> {

                        Set<String> alreadyIssuedUserIds = couponRepository.findUserIdHavingCoupon(couponId, userIds);
                        Set<String> hasToInsertUserIds = Sets.difference(new HashSet<>(userIds), alreadyIssuedUserIds);

                        List<UserCoupon> userCoupons = hasToInsertUserIds.stream()
                                .map(userId -> userCouponMapper.toEntity(userId, couponId))
                                .collect(toList());

                        userCouponRepository.saveAll(userCoupons);
                        couponRepository.increaseBatchIssuedQuantity(couponId, userCoupons.size());
                    });
        });

    }


    // 선점 상태 검증
    private void validateReservationPreemption(ReservationCoupon reservationCoupon) {
        if (reservationCoupon.getReservationCouponStatus() != ReservationCouponStatus.PREEMPTION) {
            throw new CustomCouponException(ErrorCode.RESERVATION_COUPON_NOT_PREEMPTED);
        }
    }

    // User & Role 체크
    private void validateUserAndRole(String userId, Role role, String dbUserId) {
        if (role == Role.USER) {
            if (!dbUserId.equals(userId)) {
                throw new CustomCouponException(ErrorCode.USER_NOT_MATCHED);
            }
        }
    }

    // 예약 상태 처리
    private void handleReservation(ReservationCoupon reservationCoupon, ReservationCouponStatus reservationCouponStatus) {
        switch (reservationCouponStatus) {
            case COMPLETED -> handleReservationCompleted(reservationCoupon);
            case CANCEL -> handleReservationCanceled(reservationCoupon);
            default -> throw new CustomCouponException(ErrorCode.RESERVATION_STATUS_NOT_MATCH);
        }
    }

    // 예약 완료 처리
    private void handleReservationCompleted(ReservationCoupon reservationCoupon) {
        UserCoupon userCoupon = reservationCoupon.getUserCoupon();

        // 쿠폰 상태 및 완료 처리
        userCoupon.reservationComplete();
        reservationCoupon.confirmReservationStatus(ReservationCouponStatus.COMPLETED);
    }

    // 예약 취소 처리
    private void handleReservationCanceled(ReservationCoupon reservationCoupon) {
        UserCoupon userCoupon = reservationCoupon.getUserCoupon();
        Coupon coupon = userCoupon.getCoupon();

        // 만료 시간 기준 상태 결정
        ReservationCouponStatus status = LocalDateTime.now().isAfter(coupon.getExpiredDate()) ?
                ReservationCouponStatus.EXPIRED : ReservationCouponStatus.CANCEL;

        // 쿠폰 상태 및 취소 처리
        userCoupon.reservationCancel();
        reservationCoupon.confirmReservationStatus(status);
    }

    // user 쿠폰이 reservationCoupon 테이블에 CANCEL, ROLLBACK 상태가 아닌 다른 상태값이 DB에 있으면 사용불가
    private void checkReservedCouponAvailable(UserCoupon userCoupon) {

        if (reservationCouponRepository.existsByUserCouponAndReservationCouponStatusNotInAndIsDeletedFalse(
                userCoupon, List.of(ReservationCouponStatus.ROLLBACK, ReservationCouponStatus.CANCEL))) {
            throw new CustomCouponException(ErrorCode.COUPON_UNAVAILABLE);
        }
    }

    // UserCoupon 조회 메소드
    private UserCoupon findUserCoupon(String userId, Coupon coupon) {
        return userCouponRepository.findByUserIdAndCouponIdAndIsDeletedFalse(userId, coupon.getId())
                .orElseThrow(() -> new CustomCouponException(ErrorCode.USER_COUPON_NOT_FOUND));
    }

    // 쿠폰 상태가 USED인지 확인
    private void checkIfCouponUsed(UserCoupon userCoupon) {
        if (userCoupon.checkUserCouponUsed()) {
            throw new CustomCouponException(ErrorCode.COUPON_ALREADY_USED);
        }
    }

    // 할인 금액 연산
    private double getDiscountedPrice(Coupon coupon, double totalPrice) {
        if (coupon.checkDiscountPolicy()) {
            return calculatePercentageDiscount(coupon, totalPrice);
        } else {
            return calculateFixedDiscount(coupon);
        }
    }

    // 퍼센트 할인 계산
    private double calculatePercentageDiscount(Coupon coupon, double totalPrice) {
        if (coupon.getDiscountRate() == null) {
            throw new CustomCouponException(ErrorCode.DISCOUNT_POLICY_NOT_MATCH);
        }
        return (totalPrice * coupon.getDiscountRate().getRate()) / 100.0;
    }

    // 고정 할인 금액 계산
    private double calculateFixedDiscount(Coupon coupon) {
        if (coupon.getDiscountAmount() == null) {
            throw new CustomCouponException(ErrorCode.DISCOUNT_POLICY_NOT_MATCH);
        }
        return coupon.getDiscountAmount();
    }

    private void validateBeforeReservation(Coupon coupon, FeignVerifyRequest request) {
        validateCouponDate(coupon.getAvailableDate(), coupon.getExpiredDate(), LocalDateTime.now());
        validateCouponAmount(coupon, request.reservationPrice());
    }

    // 쿠폰 금액 체크
    private void validateCouponAmount(Coupon coupon, double reservationPrice) {
        if (coupon.getMinAvailableAmount() > reservationPrice || coupon.getMaxAvailableAmount() < reservationPrice) {
            throw new CustomCouponException(ErrorCode.INVALID_PAYMENT_AMOUNT_FOR_COUPON);
        }
    }

    // 현재시간 쿠폰 시간 유효성 검사
    private void validateCouponDate(LocalDateTime availableDate, LocalDateTime expiredDate, LocalDateTime now) {
        if (now.isBefore(availableDate) || now.isAfter(expiredDate)) {
            throw new CustomCouponException(ErrorCode.COUPON_USED_WRONG_TIME);
        }
    }

    // 이벤트 상태 변경 consumer, redis 삭제, front db에 담을 데이터 요청 log로 대신함
    @Transactional
    @Override
    public void updateEndEventStatus(Set<EventClosedDto> eventClosedDtoSet) {

        Map<EventStatus, Set<String>> eventIdsByStatus = eventClosedDtoSet.stream()
                .collect
                        (groupingBy(EventClosedDto::getStatus, mapping(EventClosedDto::getEventId, toSet())));

        Map<EventStatus, List<String>> updatedEventLogs = new ConcurrentHashMap<>();

        eventIdsByStatus.forEach((eventStatus, value) -> {

            StreamSupport.stream(Iterables.partition(value, 200).spliterator(), false)
                    .forEach(eventIds -> {

                        Set<CheckAlreadyClosedEventDto> notClosedEvents = couponEventRepository.findIdsByIdInAndStatus(eventIds, EventStatus.DEFAULT);

                        if (!notClosedEvents.isEmpty()) {

                            List<String> notClosedEventIds = notClosedEvents.stream()
                                    .map(CheckAlreadyClosedEventDto::getEventId)
                                    .toList();

                            couponEventRepository.updateStatusForIds(eventStatus, notClosedEventIds);

                            updatedEventLogs.computeIfAbsent(eventStatus, k -> new CopyOnWriteArrayList<>())
                                    .addAll(notClosedEventIds);

                            redisRepository.deleteEventClosed(notClosedEvents.stream().toList());
                        }

                    });
        });
        // 현재 프로젝트에는 Front가 없지만 front에 Front 개발자가 front db에 저장을 한다는 가정
        //이 이벤트의 요청을 감소 시키기 위함
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                updatedEventLogs.forEach((status, eventIds) -> {
                    if (!eventIds.isEmpty()) {
                        log.info("Send To front DB -> Updated events : {}, status : {}", eventIds, status);
                    }
                });
            }
        });
    }

    // 쿠폰 DB check
    private Coupon checkExistsCoupon(String couponId) {

        return couponRepository.findByIdAndIsDeletedFalse(couponId)
                .orElseThrow(() -> new CustomCouponException(ErrorCode.COUPON_NOT_FOUND));
    }

    // event 시작, 끝 날짜 체크
    private void checkEventDate(CouponEvent event, LocalDateTime now) {

        if (now.isBefore(event.getEventStartDate())) {
            throw new CustomCouponException(ErrorCode.CURRENT_EVENT_NOT_STARTED);
        }

        if (!now.isBefore(event.getEventEndDate())) {
            // kafka send 명확해서 after commit이 없어도된다.
            couponErrorProducer.sendEventClosed(eventMapper.toProduce(event.getId(), EventStatus.EXPIRED));
            throw new CustomCouponException(ErrorCode.CURRENT_EVENT_CLOSED);
        }

    }

    // 발급된 쿠폰수가 최대 발급 수량보다 클때 이벤트 종료
    private void checkPriorityCouponStock(String eventId, String couponId, String userId, LocalDateTime eventEndDate) {

        CouponIssueRedisCode redisCode = redisRepository.issuePriorityCoupon(eventId, couponId, userId, eventEndDate);

        if (CouponIssueRedisCode.EXCEEDED_LIMIT == redisCode) {
            couponErrorProducer.sendEventClosed(eventMapper.toProduce(eventId, EventStatus.OUT_OF_STOCK));
            throw new CustomCouponException(ErrorCode.CURRENT_EVENT_END_TO_OUT_OF_STOCK);
        }

    }


}
