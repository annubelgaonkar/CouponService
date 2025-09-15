package dev.anuradha.couponservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.anuradha.couponservice.dto.*;
import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.model.CouponType;
import dev.anuradha.couponservice.repositories.CouponRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository repo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Coupon create(Coupon coupon) {
        return repo.save(coupon);
    }

    public List<Coupon> listAll() {
        return repo.findAll();
    }

    public Optional<Coupon> findById(String id) {
        return repo.findById(id);
    }

    @Transactional
    public Optional<Coupon> update(String id, Coupon updated) {
        return repo.findById(id).map(existing -> {
            existing.setCode(updated.getCode());
            existing.setType(updated.getType());
            existing.setDetails(updated.getDetails());
            existing.setActive(updated.isActive());
            existing.setExpiresAt(updated.getExpiresAt());
            existing.setUpdatedAt(java.time.Instant.now());
            return existing;
        });
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    //calculate discount

    public BigDecimal evaluateDiscountForCoupon(Coupon coupon, CartDto cart) {
        if (!coupon.isActive()) return BigDecimal.ZERO;
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt()
                .isBefore(java.time.Instant.now())) return BigDecimal.ZERO;

        try {
            if (coupon.getType() == CouponType.CART) {
                CartWiseDetailsDto details = objectMapper.readValue(coupon.getDetails(),
                        CartWiseDetailsDto.class);
                return evaluateCartWise(details, cart);

            } else if (coupon.getType() == CouponType.PRODUCT) {
                ProductWiseDetailsDto detailsDto = objectMapper.readValue(coupon.getDetails(),
                        ProductWiseDetailsDto.class);
                return evaluateProductWise(detailsDto, cart);

            } else if (coupon.getType() == CouponType.BXGY) {
                BxGyDetailsDto bxGyDetails = objectMapper.readValue(coupon.getDetails(),
                        BxGyDetailsDto.class);
                return evaluateBxGy(bxGyDetails, cart);
            } else {
                return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            // parsing error - treat as non-applicable
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal evaluateCartWise(CartWiseDetailsDto detailsDto, CartDto cart) {
        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(detailsDto.getThreshold()) >= 0) {
            if ("PERCENT".equalsIgnoreCase(detailsDto.getDiscountType())) {
                return total.multiply(detailsDto.getDiscountValue()).divide(BigDecimal.valueOf(100));
            } else {
                return detailsDto.getDiscountValue();
            }
        } else return BigDecimal.ZERO;
    }

    private BigDecimal evaluateProductWise(ProductWiseDetailsDto d, CartDto cart) {
        BigDecimal discount = BigDecimal.ZERO;
        for (CartItemDto item : cart.getItems()) {
            if (Objects.equals(item.getProductId(), d.getProductId())) {
                if ("PERCENT".equalsIgnoreCase(d.getDiscountType())) {
                    BigDecimal itemTotal = item.getPrice().multiply(BigDecimal
                            .valueOf(item.getQuantity()));
                    discount = discount.add(itemTotal.multiply(d.getDiscountValue())
                            .divide(BigDecimal.valueOf(100)));
                } else {
                    // FLAT per unit or total? We'll assume FLAT per unit * quantity.
                    discount = discount.add(d.getDiscountValue()
                            .multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }
        return discount;
    }

    private BigDecimal evaluateBxGy(BxGyDetailsDto detailsDto, CartDto cart) {
        // Simple deterministic greedy approach:
        // 1) Count total buyUnits available from buyProducts
        // 2) Determine how many times promotion repeats = floor(totalBuyUnits / requiredBuyUnits) limited by repetitionLimit
        // 3) For each repetition, sum price of matching getProducts available in cart (we'll give free units from matching get products)
        Map<Long, Integer> cartQty = cart.getItems().stream()
                .collect(Collectors.toMap(CartItemDto::getProductId,
                        CartItemDto::getQuantity, Integer::sum));

        int totalBuyRequired = detailsDto.getBuyProducts().stream()
                .mapToInt(BxGyDetailsDto.BuyProduct::getQuantity).sum();
        int totalBuyPresent = detailsDto.getBuyProducts().stream()
                .mapToInt(bp -> cartQty.getOrDefault(bp.getProductId(),
                        0) / bp.getQuantity())
                .sum() * 1; // this counts repeatable sets per product - simpler approach below

        // Simpler approach: compute total buy units across buyProducts by summing quantities
        int totalBuyUnits = detailsDto.getBuyProducts().stream()
                .mapToInt(bp -> cartQty.getOrDefault(bp.getProductId(), 0))
                .sum();

        int buyRequiredPerApply = detailsDto.getBuyProducts().stream().mapToInt(BxGyDetailsDto
                .BuyProduct::getQuantity).sum();
        if (buyRequiredPerApply <= 0) return BigDecimal.ZERO;

        int possibleReps = totalBuyUnits / buyRequiredPerApply;
        if (d.getRepetitionLimit() != null) {
            possibleReps = Math.min(possibleReps, detailsDto.getRepetitionLimit());
        }
        if (possibleReps <= 0) return BigDecimal.ZERO;

        // Determine how many get units are present in cart to make free
        int totalGetUnitsAvailable = detailsDto.getGetProducts().stream()
                .mapToInt(gp -> cartQty.getOrDefault(gp.getProductId(), 0))
                .sum();

        int totalGetUnitsPerApply = detailsDto.getGetProducts().stream().mapToInt(BxGyDetailsDto
                .GetProduct::getQuantity).sum();
        int totalFreeUnits = Math.min(totalGetUnitsAvailable, possibleReps * totalGetUnitsPerApply);
        if (totalFreeUnits <= 0) return BigDecimal.ZERO;

        // To compute discount amount, choose deterministic mapping: prefer getProducts in the order given.
        BigDecimal discount = BigDecimal.ZERO;
        int remainingFree = totalFreeUnits;
        // create a map productId -> price from cart
        Map<Long, BigDecimal> priceMap = cart.getItems().stream()
                .collect(Collectors.toMap(CartItemDto::getProductId,
                        CartItemDto::getPrice, (p1,p2)->p1));
        for (BxGyDetailsDto.GetProduct gp : detailsDto.getGetProducts()) {
            if (remainingFree <= 0) break;
            int available = cartQty.getOrDefault(gp.getProductId(), 0);
            int toFree = Math.min(available, Math.min(gp.getQuantity() * possibleReps, remainingFree));
            if (toFree > 0) {
                BigDecimal price = priceMap.getOrDefault(gp.getProductId(), BigDecimal.ZERO);
                discount = discount.add(price.multiply(BigDecimal.valueOf(toFree)));
                remainingFree -= toFree;
            }
        }
        return discount;
    }

    // Evaluate all coupons and return list of coupon -> discount
    public Map<String, BigDecimal> applicableCouponsForCart(CartDto cart) {
        List<Coupon> coupons = repo.findAll();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Coupon c : coupons) {
            BigDecimal d = evaluateDiscountForCoupon(c, cart);
            if (d.compareTo(BigDecimal.ZERO) > 0) {
                result.put(c.getId(), d);
            }
        }
        return result;
    }

    // Apply a specific coupon and return updated cart (mutating CartDto items' totalDiscount)
    public CartDto applyCouponToCart(Coupon coupon, CartDto cart) {
        BigDecimal totalDiscount = evaluateDiscountForCoupon(coupon, cart);

        // Reset item-level discount
        cart.getItems().forEach(it -> it.setTotalDiscount(BigDecimal.ZERO));

        try {
            if (coupon.getType() == CouponType.CART) {
                CartWiseDetailsDto detailsDto = objectMapper.readValue(coupon.getDetails(), CartWiseDetailsDto.class);
                // distribute discount proportionally to item totals
                BigDecimal total = cart.getItems().stream()
                        .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (total.compareTo(BigDecimal.ZERO) > 0 && totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    for (CartItemDto item : cart.getItems()) {
                        BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                        BigDecimal share = itemTotal.divide(total, 6,
                                BigDecimal.ROUND_HALF_UP).multiply(totalDiscount);
                        item.setTotalDiscount(share);
                    }
                }
            } else if (coupon.getType() == CouponType.PRODUCT) {
                ProductWiseDetailsDto productWiseDetails = objectMapper.readValue(coupon.getDetails(),
                        ProductWiseDetailsDto.class);
                for (CartItemDto item : cart.getItems()) {
                    if (Objects.equals(item.getProductId(), productWiseDetails.getProductId())) {
                        if ("PERCENT".equalsIgnoreCase(productWiseDetails.getDiscountType())) {
                            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                            BigDecimal disc = itemTotal.multiply(productWiseDetails.getDiscountValue())
                                    .divide(BigDecimal.valueOf(100));
                            item.setTotalDiscount(disc);
                        } else {
                            BigDecimal disc = productWiseDetails
                                    .getDiscountValue().multiply(BigDecimal.valueOf(item.getQuantity()));
                            item.setTotalDiscount(disc);
                        }
                    }
                }
            } else if (coupon.getType() == CouponType.BXGY) {
                BxGyDetailsDto detailsDto = objectMapper.readValue(coupon.getDetails(), BxGyDetailsDto.class);
                // Similar to evaluation: mark get products free (deterministic order)
                Map<Long, Integer> cartQty = cart.getItems().stream()
                        .collect(Collectors.toMap(CartItemDto::getProductId, CartItemDto::getQuantity, Integer::sum));

                int totalBuyUnits = detailsDto.getBuyProducts().stream()
                        .mapToInt(bp -> cartQty.getOrDefault(bp.getProductId(), 0))
                        .sum();
                int buyRequired = detailsDto.getBuyProducts().stream()
                        .mapToInt(BxGyDetailsDto.BuyProduct::getQuantity).sum();
                if (buyRequired <= 0) return cart;
                int possibleReps = totalBuyUnits / buyRequired;
                if (detailsDto.getRepetitionLimit() != null) possibleReps =
                        Math.min(possibleReps, detailsDto.getRepetitionLimit());
                int totalGetUnitsPerApply = detailsDto.getGetProducts().stream().
                        mapToInt(BxGyDetailsDto.GetProduct::getQuantity).sum();
                int totalFreeUnits = Math.min(detailsDto.getGetProducts().stream()
                        .mapToInt(gp -> cartQty.getOrDefault(gp.getProductId(),
                                0)).sum(), possibleReps * totalGetUnitsPerApply);
                if (totalFreeUnits <= 0) return cart;

                int remainingFree = totalFreeUnits;
                // map productId to CartItemDto
                Map<Long, CartItemDto> cartItemMap = cart.getItems().stream()
                        .collect(Collectors.toMap(CartItemDto::getProductId, i -> i, (a, b)->a));
                for (BxGyDetailsDto.GetProduct gp : detailsDto.getGetProducts()) {
                    if (remainingFree <= 0) break;
                    CartItemDto item = cartItemMap.get(gp.getProductId());
                    if (item == null) continue;
                    int toFree = Math.min(item.getQuantity(), Math.min(gp.getQuantity() * possibleReps, remainingFree));
                    if (toFree > 0) {
                        BigDecimal disc = item.getPrice().multiply(BigDecimal.valueOf(toFree));
                        item.setTotalDiscount(disc);
                        remainingFree -= toFree;
                    }
                }
            }
        } catch (Exception ex) {
            // ignore parse errors - leave discounts as zero
        }
        return cart;
    }


}
