package dev.anuradha.couponservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductWiseDetailsDto {

    private Long productId;
    private String discountType;
    private BigDecimal discountValue;
}
