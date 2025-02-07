package com.haot.coupon.domain.model;

import com.haot.coupon.common.exceptions.CustomCouponException;
import com.haot.coupon.common.response.enums.ErrorCode;
import com.haot.coupon.domain.model.enums.EventStatus;
import com.haot.submodule.auditor.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_coupon_event", schema = "coupon")
public class CouponEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // 추후 이벤트에 무제한 쿠폰을 돌려쓸 수도 있으니 Many to One으로
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false, name = "event_start_date")
    private LocalDateTime eventStartDate;

    @Column(nullable = false, name = "event_end_date")
    private LocalDateTime eventEndDate;

    @Column(nullable = false, name = "event_name")
    private String eventName;

    @Column(nullable = false, name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus eventStatus;

    public void updateEventStatus(EventStatus eventStatus){
        this.eventStatus = eventStatus;
    }

    public void modifyEvent(String eventName, String description){

        if(eventName != null && !eventName.isEmpty()){
            this.eventName = eventName;
        }

        if(description != null && !description.isEmpty()){
            this.description = description;
        }

    }


}
