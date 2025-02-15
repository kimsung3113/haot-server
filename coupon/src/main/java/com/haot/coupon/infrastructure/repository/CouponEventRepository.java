package com.haot.coupon.infrastructure.repository;

import com.haot.coupon.application.dto.CheckAlreadyClosedEventDto;
import com.haot.coupon.domain.model.CouponEvent;
import com.haot.coupon.domain.model.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CouponEventRepository extends JpaRepository<CouponEvent, String>, CouponEventCustomRepository {

    boolean existsByCouponIdAndIsDeletedFalse(String couponId);

    List<CouponEvent> findByCouponIdAndEventStatusAndIsDeletedFalse(String id, EventStatus status);

    @Query("SELECT ce FROM CouponEvent ce JOIN FETCH ce.coupon c " +
            "WHERE ce.id = :eventId AND ce.eventStatus = :eventStatus " +
            "AND ce.isDeleted = false AND c.isDeleted = false")
    Optional<CouponEvent> findByIdAndEventStatusAndIsDeleteFalse(@Param("eventId") String eventId,
                                                                 @Param("eventStatus") EventStatus eventStatus);

    Optional<CouponEvent> findByIdAndIsDeletedFalse(String eventId);

    @Query("SELECT new com.haot.coupon.application.dto.CheckAlreadyClosedEventDto(e.id, e.coupon.id) " +
            "FROM CouponEvent e " +
            "WHERE e.id IN :ids AND e.eventStatus = :status")
    Set<CheckAlreadyClosedEventDto> findIdsByIdInAndStatus(@Param("ids") List<String> ids, @Param("status") EventStatus status);

    @Modifying
    @Query("UPDATE CouponEvent e SET e.eventStatus = :status WHERE e.id IN :ids")
    void updateStatusForIds(@Param("status") EventStatus status, @Param("ids") List<String> ids);
}
