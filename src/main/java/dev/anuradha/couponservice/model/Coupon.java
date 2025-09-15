package dev.anuradha.couponservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@Table(name = "coupons")
public class Coupon {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "code", unique = true, nullable = false)
    private String code;                                 //This is human readable coupon code

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CouponType type;                             // cart, product, buy x get y

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "is_active")
    private boolean active = true;                        //whether the coupon is active or no

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

}
