package dev.anuradha.couponservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.anuradha.couponservice.dto.*;
import dev.anuradha.couponservice.exception.BadRequestException;
import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.model.CouponType;
import dev.anuradha.couponservice.repositories.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository repo;
    private final ObjectMapper objectMapper;

    // -------------------------
    // CRUD & validation
    // -------------------------

    public Coupon create(Coupon coupon) {
        if (coupon == null) {
            throw new BadRequestException("coupon payload is required");
        }
        if (coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            throw new BadRequestException("coupon.code is required");
        }
        if (coupon.getType() == null) {
            throw new BadRequestException("coupon.type is required");
        }

        // validate details (required on create)
        validateCouponDetailsForCreate(coupon);

        // optional: ensure unique code
        repo.findByCode(coupon.getCode()).ifPresent(existing -> {
            throw new BadRequestException("coupon code already exists: " + coupon.getCode());
        });

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
            // validate only when type or details are provided (support partial updates)
            if (updated.getType() != null || updated.getDetails() != null) {
                CouponType effectiveType = (updated.getType() != null) ? updated.getType() : existing.getType();
                String detailsToValidate = (updated.getDetails() != null) ? updated.getDetails() : existing.getDetails();
                validateCouponDetailsForUpdate(effectiveType, detailsToValidate);
            }

            if (updated.getCode() != null) existing.setCode(updated.getCode());
            if (updated.getType() != null) existing.setType(updated.getType());
            if (updated.getDetails() != null) existing.setDetails(updated.getDetails());
            existing.setActive(updated.isActive());
            existing.setExpiresAt(updated.getExpiresAt());
            existing.setUpdatedAt(Instant.now());
            return existing;
        });
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    // -------------------------
    // Validation helpers
    // -------------------------

    private void validateCouponDetailsForCreate(Coupon coupon) {
        if (coupon == null) throw new BadRequestException("coupon is required");
        if (coupon.getType() == null) throw new BadRequestException("coupon.type is required");
        if (coupon.getDetails() == null || coupon.getDetails().trim().isEmpty()) {
            throw new BadRequestException("details JSON is required for coupon type " + coupon.getType());
        }
        validateDetailsForType(coupon.getType(), coupon.getDetails());
    }

    private void validateCouponDetailsForUpdate(CouponType type, String details) {
        if (type == null) return;
        if (details == null || details.trim().isEmpty()) {
            // allow missing details on update (we'll keep existing details)
            return;
        }
        validateDetailsForType(type, details);
    }

    private void validateDetailsForType(CouponType type, String detailsJson) {
        if (type == null) throw new BadRequestException("coupon type is required for validation");
        if (detailsJson == null || detailsJson.trim().isEmpty())
            throw new BadRequestException("details JSON is required for type " + type);

        try {
            switch (type) {
                case CART -> {
                    CartWiseDetailsDto d = objectMapper.readValue(detailsJson, CartWiseDetailsDto.class);
                    if (d.getThreshold() == null || d.getDiscountType() == null || d.getDiscountValue() == null) {
                        throw new BadRequestException("Cart coupon requires threshold, discountType and discountValue");
                    }
                }
                case PRODUCT -> {
                    ProductWiseDetailsDto d = objectMapper.readValue(detailsJson, ProductWiseDetailsDto.class);
                    if (d.getProductId() == null || d.getDiscountType() == null || d.getDiscountValue() == null) {
                        throw new BadRequestException("Product coupon requires productId, discountType and discountValue");
                    }
                }
                case BXGY -> {
                    BxGyDetailsDto d = objectMapper.readValue(detailsJson, BxGyDetailsDto.class);
                    if (d.getBuyProducts() == null || d.getBuyProducts().isEmpty()
                            || d.getGetProducts() == null || d.getGetProducts().isEmpty()) {
                        throw new BadRequestException("BxGy coupon requires buyProducts and getProducts");
                    }
                    d.getBuyProducts().forEach(bp -> {
                        if (bp.getProductId() == null || bp.getQuantity() == null || bp.getQuantity() <= 0) {
                            throw new BadRequestException("each buyProduct must have productId and positive quantity");
                        }
                    });
                    d.getGetProducts().forEach(gp -> {
                        if (gp.getProductId() == null || gp.getQuantity() == null || gp.getQuantity() <= 0) {
                            throw new BadRequestException("each getProduct must have productId and positive quantity");
                        }
                    });
                }
                default -> throw new BadRequestException("Unknown coupon type: " + type);
            }
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid details JSON: " + ex.getOriginalMessage());
        }
    }

    // -------------------------
    // Evaluation logic
    // -------------------------

    public BigDecimal evaluateDiscountForCoupon(Coupon coupon, CartDto cart) {
        if (coupon == null) return BigDecimal.ZERO;
        if (!coupon.isActive()) return BigDecimal.ZERO;
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now()))
            return BigDecimal.ZERO;

        try {
            if (coupon.getType() == CouponType.CART) {
                CartWiseDetailsDto details = objectMapper.readValue(coupon.getDetails(), CartWiseDetailsDto.class);
                return evaluateCartWise(details, cart);

            } else if (coupon.getType() == CouponType.PRODUCT) {
                ProductWiseDetailsDto detailsDto = objectMapper.readValue(coupon.getDetails(), ProductWiseDetailsDto.class);
                return evaluateProductWise(detailsDto, cart);

            } else if (coupon.getType() == CouponType.BXGY) {
                BxGyDetailsDto bxGyDetails = objectMapper.readValue(coupon.getDetails(), BxGyDetailsDto.class);
                return evaluateBxGy(bxGyDetails, cart);
            } else {
                return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            // parsing or other error - treat as non-applicable
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal evaluateCartWise(CartWiseDetailsDto detailsDto, CartDto cart) {
        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (detailsDto.getThreshold() == null) return BigDecimal.ZERO;
        if (total.compareTo(detailsDto.getThreshold()) >= 0) {
            if ("PERCENT".equalsIgnoreCase(detailsDto.getDiscountType())) {
                return total.multiply(detailsDto.getDiscountValue()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
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
                    BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    discount = discount.add(itemTotal.multiply(d.getDiscountValue()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                } else {
                    // FLAT per unit assumed
                    discount = discount.add(d.getDiscountValue().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }
        return discount;
    }

    private BigDecimal evaluateBxGy(BxGyDetailsDto detailsDto, CartDto cart) {
        // Deterministic greedy approach:
        Map<Long, Integer> cartQty = cart.getItems().stream()
                .collect(Collectors.toMap(CartItemDto::getProductId, CartItemDto::getQuantity, Integer::sum));

        int buyRequiredPerApply = detailsDto.getBuyProducts().stream()
                .mapToInt(BxGyDetailsDto.BuyProduct::getQuantity).sum();
        if (buyRequiredPerApply <= 0) return BigDecimal.ZERO;

        int totalBuyUnits = detailsDto.getBuyProducts().stream()
                .mapToInt(bp -> cartQty.getOrDefault(bp.getProductId(), 0))
                .sum();

        int possibleReps = totalBuyUnits / buyRequiredPerApply;
        if (detailsDto.getRepetitionLimit() != null) {
            possibleReps = Math.min(possibleReps, detailsDto.getRepetitionLimit());
        }
        if (possibleReps <= 0) return BigDecimal.ZERO;

        int totalGetUnitsAvailable = detailsDto.getGetProducts().stream()
                .mapToInt(gp -> cartQty.getOrDefault(gp.getProductId(), 0))
                .sum();

        int totalGetUnitsPerApply = detailsDto.getGetProducts().stream()
                .mapToInt(BxGyDetailsDto.GetProduct::getQuantity).sum();

        int totalFreeUnits = Math.min(totalGetUnitsAvailable, possibleReps * totalGetUnitsPerApply);
        if (totalFreeUnits <= 0) return BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;
        int remainingFree = totalFreeUnits;

        Map<Long, BigDecimal> priceMap = cart.getItems().stream()
                .collect(Collectors.toMap(CartItemDto::getProductId, CartItemDto::getPrice, (p1, p2) -> p1));

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

    // -------------------------
    // Apply / applicable endpoints helpers
    // -------------------------

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

    /**
     * Apply a specific coupon and return updated cart (mutates CartDto items' totalDiscount).
     */
    public CartDto applyCouponToCart(Coupon coupon, CartDto cart) {
        BigDecimal totalDiscount = evaluateDiscountForCoupon(coupon, cart);

        // Reset item-level discount
        cart.getItems().forEach(it -> it.setTotalDiscount(BigDecimal.ZERO));

        try {
            if (coupon.getType() == CouponType.CART) {
                CartWiseDetailsDto detailsDto = objectMapper.readValue(coupon.getDetails(), CartWiseDetailsDto.class);
                BigDecimal total = cart.getItems().stream()
                        .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (total.compareTo(BigDecimal.ZERO) > 0 && totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    for (CartItemDto item : cart.getItems()) {
                        BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                        BigDecimal share = itemTotal.divide(total, 6, RoundingMode.HALF_UP).multiply(totalDiscount);
                        item.setTotalDiscount(share);
                    }
                }
            } else if (coupon.getType() == CouponType.PRODUCT) {
                ProductWiseDetailsDto productWiseDetails = objectMapper.readValue(coupon.getDetails(), ProductWiseDetailsDto.class);
                for (CartItemDto item : cart.getItems()) {
                    if (Objects.equals(item.getProductId(), productWiseDetails.getProductId())) {
                        if ("PERCENT".equalsIgnoreCase(productWiseDetails.getDiscountType())) {
                            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                            BigDecimal disc = itemTotal.multiply(productWiseDetails.getDiscountValue()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                            item.setTotalDiscount(disc);
                        } else {
                            BigDecimal disc = productWiseDetails.getDiscountValue().multiply(BigDecimal.valueOf(item.getQuantity()));
                            item.setTotalDiscount(disc);
                        }
                    }
                }
            } else if (coupon.getType() == CouponType.BXGY) {
                BxGyDetailsDto detailsDto = objectMapper.readValue(coupon.getDetails(), BxGyDetailsDto.class);
                Map<Long, Integer> cartQty = cart.getItems().stream()
                        .collect(Collectors.toMap(CartItemDto::getProductId, CartItemDto::getQuantity, Integer::sum));

                int totalBuyUnits = detailsDto.getBuyProducts().stream()
                        .mapToInt(bp -> cartQty.getOrDefault(bp.getProductId(), 0))
                        .sum();
                int buyRequired = detailsDto.getBuyProducts().stream()
                        .mapToInt(BxGyDetailsDto.BuyProduct::getQuantity).sum();
                if (buyRequired <= 0) return cart;
                int possibleReps = totalBuyUnits / buyRequired;
                if (detailsDto.getRepetitionLimit() != null)
                    possibleReps = Math.min(possibleReps, detailsDto.getRepetitionLimit());

                int totalGetUnitsPerApply = detailsDto.getGetProducts().stream()
                        .mapToInt(BxGyDetailsDto.GetProduct::getQuantity).sum();

                int totalFreeUnits = Math.min(detailsDto.getGetProducts().stream()
                        .mapToInt(gp -> cartQty.getOrDefault(gp.getProductId(), 0)).sum(), possibleReps * totalGetUnitsPerApply);
                if (totalFreeUnits <= 0) return cart;

                int remainingFree = totalFreeUnits;
                Map<Long, CartItemDto> cartItemMap = cart.getItems().stream()
                        .collect(Collectors.toMap(CartItemDto::getProductId, i -> i, (a, b) -> a));

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
            // leave discounts as zero
        }
        return cart;
    }
}
