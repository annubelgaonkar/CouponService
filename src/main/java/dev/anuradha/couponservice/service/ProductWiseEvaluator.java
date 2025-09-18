package dev.anuradha.couponservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.anuradha.couponservice.dto.CartDto;
import dev.anuradha.couponservice.dto.CartItemDto;
import dev.anuradha.couponservice.dto.ProductWiseDetailsDto;
import dev.anuradha.couponservice.model.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProductWiseEvaluator implements Evaluator{

    private final ObjectMapper objectMapper;

    @Override
    public BigDecimal evaluate(Coupon coupon, CartDto cartDto){
        // product-wise evaluation logic
        try{
            ProductWiseDetailsDto detailsDto = objectMapper.readValue(
                    coupon.getDetails(), ProductWiseDetailsDto.class
            );
            BigDecimal discount = BigDecimal.ZERO;
            for(CartItemDto itemDto : cartDto.getItems()){
                if(Objects.equals(itemDto.getProductId(), detailsDto.getProductId())){
                    if("PERCENT".equalsIgnoreCase(detailsDto.getDiscountType())){
                        BigDecimal itemTotal = itemDto.getPrice()
                                .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                        discount = discount.add(itemTotal.multiply(detailsDto.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                    }else {
                        discount = discount.add(detailsDto.getDiscountValue()
                                .multiply(BigDecimal.valueOf(itemDto.getQuantity())));
                    }
                }
            }
            return discount;
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void apply(Coupon coupon, CartDto cartDto){
        try{
            ProductWiseDetailsDto detailsDto =
                    objectMapper.readValue(coupon.getDetails(), ProductWiseDetailsDto.class);
            for(CartItemDto cartItemDto : cartDto.getItems()){
                if(Objects.equals(cartItemDto.getProductId(), detailsDto.getProductId())){
                    if("PERCENT".equalsIgnoreCase(detailsDto.getDiscountType())){
                        BigDecimal itemTotal = cartItemDto.getPrice()
                                .multiply(BigDecimal.valueOf(cartItemDto.getQuantity()));
                        BigDecimal disc = itemTotal.multiply(detailsDto.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                        cartItemDto.setTotalDiscount(disc);
                    }else {
                        BigDecimal disc = detailsDto.getDiscountValue()
                                .multiply(BigDecimal.valueOf(cartItemDto.getQuantity()));
                        cartItemDto.setTotalDiscount(disc);
                    }
                }
                else {
                    cartItemDto.setTotalDiscount(cartItemDto.getTotalDiscount() == null ?
                            BigDecimal.ZERO : cartItemDto.getTotalDiscount());
                }
            }
        } catch (Exception e) {
            //leave discount as zero
        }
    }
}
