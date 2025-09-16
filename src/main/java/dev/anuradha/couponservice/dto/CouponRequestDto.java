package dev.anuradha.couponservice.dto;

import dev.anuradha.couponservice.model.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@AllArgsConstructor
public class CouponRequestDto {

    @NotBlank
    private String code;

    @NotNull
    private CouponType type;

    @NotBlank
    private String details;

    private boolean active = true;

    private Instant expiresAt;
}
