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
public class CartItemDto {

    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalDiscount;
}
