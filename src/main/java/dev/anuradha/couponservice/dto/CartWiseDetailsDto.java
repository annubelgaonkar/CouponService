package dev.anuradha.couponservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CartWiseDetailsDto {

    private BigDecimal threshold;

    // % or FLAT
    private String discountType;
    private BigDecimal discountValue;
}
