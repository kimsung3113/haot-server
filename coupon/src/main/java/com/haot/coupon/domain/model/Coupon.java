package com.haot.coupon.domain.model;

import com.haot.coupon.domain.model.enums.CouponType;
import com.haot.coupon.domain.model.enums.DiscountPolicy;
import com.haot.coupon.domain.model.vo.CouponDiscountRate;
import com.haot.submodule.auditor.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_coupon", schema = "coupon")
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, name = "name")
    private String name;

    @Column(nullable = false, name = "total_quantity")
    private Integer totalQuantity;

    @Column(nullable = false, name = "issued_quantity")
    private Integer issuedQuantity;

    @Column(nullable = false, name = "type")
    @Enumerated(EnumType.STRING)
    private CouponType type;

    @Column(nullable = false, name = "discount_policy")
    @Enumerated(EnumType.STRING)
    private DiscountPolicy discountPolicy;

    @Column(nullable = false, name = "min_available_amount")
    private Double minAvailableAmount;

    @Column(nullable = false, name = "max_available_amount")
    private Double maxAvailableAmount;

    @Column(nullable = false, name = "available_date")
    private LocalDateTime availableDate;

    @Column(nullable = false, name = "expired_date")
    private LocalDateTime expiredDate;

    @Embedded
    private CouponDiscountRate discountRate;

    @Column(nullable = true, name = "discount_amount")
    private Double discountAmount;

    public void issue(){
        this.issuedQuantity++;
    }

    public boolean checkPriorityCoupon(){
        return this.totalQuantity != -1 && this.type == CouponType.PRIORITY;
    }

    public boolean checkDiscountPolicy(){
        return this.discountPolicy == DiscountPolicy.PERCENTAGE;
    }

}
