package dev.anuradha.couponservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.anuradha.couponservice.dto.CartDto;
import dev.anuradha.couponservice.dto.CartItemDto;
import dev.anuradha.couponservice.dto.CartWiseDetailsDto;
import dev.anuradha.couponservice.model.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class CartWiseEvaluator implements Evaluator{

    private final ObjectMapper objectMapper;

    @Override
    public BigDecimal evaluate(Coupon coupon, CartDto cartDto){
        //cart-wise evaluation logic
        try{
            CartWiseDetailsDto detailsDto = objectMapper.readValue(
                    coupon.getDetails(), CartWiseDetailsDto.class
            );
            BigDecimal total = cartDto.getItems().stream()
                    .map(item -> item.getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (detailsDto.getThreshold() == null) return BigDecimal.ZERO;
            if (total.compareTo(detailsDto.getThreshold()) >= 0) {
                if ("PERCENT".equalsIgnoreCase(detailsDto.getDiscountType())) {
                    return total.multiply(detailsDto.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                } else {
                    return detailsDto.getDiscountValue();
                }
            }
        } catch (Exception e) {
           // treat as non-applicable
        }
        return BigDecimal.ZERO;
    }

    @Override
    public void apply(Coupon coupon, CartDto cartDto){
        BigDecimal totalDiscount = evaluate(coupon, cartDto);
        if(totalDiscount.compareTo(BigDecimal.ZERO) <= 0)    return;

        BigDecimal total = cartDto.getItems().stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) <= 0) return;

        for (CartItemDto item : cartDto.getItems()) {
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal share = itemTotal.divide(total, 6,
                    RoundingMode.HALF_UP).multiply(totalDiscount);
            item.setTotalDiscount(share);
        }
    }
}
