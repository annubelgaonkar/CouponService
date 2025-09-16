package dev.anuradha.couponservice.dto;

import dev.anuradha.couponservice.model.CouponType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CouponResponseDto {

    private String id;
    private String code;
    private CouponType type;
    private String details;
    private boolean active;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
