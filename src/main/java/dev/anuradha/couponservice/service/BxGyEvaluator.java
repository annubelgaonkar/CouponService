package dev.anuradha.couponservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.anuradha.couponservice.dto.BxGyDetailsDto;
import dev.anuradha.couponservice.dto.CartDto;
import dev.anuradha.couponservice.dto.CartItemDto;
import dev.anuradha.couponservice.model.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BxGyEvaluator implements Evaluator{

    private final ObjectMapper objectMapper;

    @Override
    public BigDecimal evaluate(Coupon coupon, CartDto cartDto){
        // bxgy evaluation logic
        try {
            BxGyDetailsDto detailsDto = objectMapper
                    .readValue(coupon.getDetails(), BxGyDetailsDto.class);

            Map<Long, Integer> cartQty = cartDto.getItems().stream()
                    .collect(Collectors.toMap(CartItemDto::getProductId,
                            CartItemDto::getQuantity, Integer::sum));

            int buyRequiredPerApply = detailsDto.getBuyProducts().stream()
                    .mapToInt(BxGyDetailsDto.BuyProduct::getQuantity).sum();
            if (buyRequiredPerApply <= 0) return BigDecimal.ZERO;

            int totalBuyUnits = detailsDto.getBuyProducts().stream()
                    .mapToInt(bp -> cartQty.getOrDefault(bp.getProductId(),
                            0))
                    .sum();

            int possibleReps = totalBuyUnits / buyRequiredPerApply;
            if (detailsDto.getRepetitionLimit() != null) {
                possibleReps = Math.min(possibleReps, detailsDto.getRepetitionLimit());
            }
            if (possibleReps <= 0) return BigDecimal.ZERO;

            int totalGetUnitsAvailable = detailsDto.getGetProducts().stream()
                    .mapToInt(gp -> cartQty.getOrDefault(gp
                            .getProductId(), 0))
                    .sum();

            int totalGetUnitsPerApply = detailsDto.getGetProducts().stream()
                    .mapToInt(BxGyDetailsDto.GetProduct::getQuantity).sum();

            int totalFreeUnits = Math.min(totalGetUnitsAvailable, possibleReps * totalGetUnitsPerApply);
            if (totalFreeUnits <= 0) return BigDecimal.ZERO;

            Map<Long, BigDecimal> priceMap = cartDto.getItems().stream()
                    .collect(Collectors.toMap(CartItemDto::getProductId,
                            CartItemDto::getPrice, (p1, p2) -> p1));

            BigDecimal discount = BigDecimal.ZERO;
            int remainingFree = totalFreeUnits;
            for (BxGyDetailsDto.GetProduct gp : detailsDto.getGetProducts()) {
                if (remainingFree <= 0) break;
                int available = cartQty.getOrDefault(gp.getProductId(),
                        0);
                int toFree = Math.min(available, Math.min(gp.getQuantity() * possibleReps, remainingFree));
                if (toFree > 0) {
                    BigDecimal price = priceMap.getOrDefault(gp.getProductId(), BigDecimal.ZERO);
                    discount = discount.add(price.multiply(BigDecimal.valueOf(toFree)));
                    remainingFree -= toFree;
                }
            }
            return discount;
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }

    }

    @Override
    public void apply(Coupon coupon, CartDto cart) {
        try {
            BxGyDetailsDto detailsDto = objectMapper.readValue(coupon.getDetails(),
                    BxGyDetailsDto.class);

            Map<Long, Integer> cartQty = cart.getItems().stream()
                    .collect(Collectors.toMap(CartItemDto::getProductId,
                            CartItemDto::getQuantity, Integer::sum));

            int buyRequiredPerApply = detailsDto.getBuyProducts().stream()
                    .mapToInt(BxGyDetailsDto.BuyProduct::getQuantity).sum();
            if (buyRequiredPerApply <= 0) return;

            int totalBuyUnits = detailsDto.getBuyProducts().stream()
                    .mapToInt(bp -> cartQty.getOrDefault(bp.getProductId(),
                            0))
                    .sum();

            int possibleReps = totalBuyUnits / buyRequiredPerApply;
            if (detailsDto.getRepetitionLimit() != null) {
                possibleReps = Math.min(possibleReps, detailsDto.getRepetitionLimit());
            }
            if (possibleReps <= 0) return;

            int totalGetUnitsPerApply = detailsDto.getGetProducts().stream()
                    .mapToInt(BxGyDetailsDto.GetProduct::getQuantity).sum();

            int totalFreeUnits = Math.min(detailsDto.getGetProducts().stream()
                    .mapToInt(gp -> cartQty.getOrDefault(gp.getProductId(),
                            0)).sum(), possibleReps * totalGetUnitsPerApply);
            if (totalFreeUnits <= 0) return;

            int remainingFree = totalFreeUnits;
            Map<Long, CartItemDto> cartItemMap = cart.getItems().stream()
                    .collect(Collectors.toMap(CartItemDto::getProductId, i -> i,
                            (a, b) -> a));

            for (BxGyDetailsDto.GetProduct gp : detailsDto.getGetProducts()) {
                if (remainingFree <= 0) break;
                CartItemDto item = cartItemMap.get(gp.getProductId());
                if (item == null) continue;
                int toFree = Math.min(item.getQuantity(),
                        Math.min(gp.getQuantity() * possibleReps, remainingFree));
                if (toFree > 0) {
                    java.math.BigDecimal disc = item.getPrice()
                            .multiply(java.math.BigDecimal.valueOf(toFree));
                    item.setTotalDiscount(disc);
                    remainingFree -= toFree;
                }
            }
        } catch (Exception ex) {
            // leave discounts as zero
        }
    }
}
