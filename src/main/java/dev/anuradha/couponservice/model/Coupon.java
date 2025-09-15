package dev.anuradha.couponservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "coupons")
public class Coupon {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @NotBlank
    @Column(name = "code", unique = true, nullable = false)
    private String code;                                 //This is human readable coupon code

    @NotBlank
    private String type;                                 // cart, product, buy x get y

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "is_active")
    private boolean active = true;                        //whether the coupon is active or no

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Coupon() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

}
