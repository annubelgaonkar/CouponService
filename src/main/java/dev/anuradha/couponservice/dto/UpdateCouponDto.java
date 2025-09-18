package dev.anuradha.couponservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCouponDto {
    private String code;
    private String type;
    private String details;
    private Boolean active;     // wrapper - null if omitted
    private Instant expiresAt;
}
