package dev.anuradha.couponservice.dto;

import dev.anuradha.couponservice.model.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CouponRequestDto {

    @NotBlank
    private String code;

    @NotNull
    private CouponType type;

    @NotBlank
    private String details;

    private boolean active = true;
}
