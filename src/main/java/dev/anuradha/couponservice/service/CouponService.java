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
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final Map<CouponType, Evaluator> evaluatorMap;

    private final CouponRepository repo;
    private final ObjectMapper objectMapper;

    // CRUD & validation

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

        validateCouponDetailsForCreate(coupon);

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
    public Optional<Coupon> update(String id, UpdateCouponDto dto) {
        return repo.findById(id).map(existing -> {
            // Validate only when type/details provided
            if (dto.getType() != null || dto.getDetails() != null) {
                CouponType effectiveType = (dto.getType() != null) ? CouponType.valueOf(dto.getType()) : existing.getType();
                String detailsToValidate = (dto.getDetails() != null) ? dto.getDetails() : existing.getDetails();
                validateCouponDetailsForUpdate(effectiveType, detailsToValidate);
            }

            if (dto.getCode() != null) existing.setCode(dto.getCode());
            if (dto.getType() != null) existing.setType(CouponType.valueOf(dto.getType()));
            if (dto.getDetails() != null) existing.setDetails(dto.getDetails());
            if (dto.getActive() != null) existing.setActive(dto.getActive()); // safe
            if (dto.getExpiresAt() != null) existing.setExpiresAt(dto.getExpiresAt());

            existing.setUpdatedAt(Instant.now());
            return existing;
        });
    }

    public void delete(String id) {
        repo.deleteById(id);
    }


    // Validation helpers
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


    /** Evaluation & apply logic handled by Evaluators
     * Evaluate discount for a given coupon and cart.
     * handled by the respective Evaluator for the coupon type.
     */

    public BigDecimal evaluateDiscountForCoupon(Coupon coupon, CartDto cart) {
        if (coupon == null) return BigDecimal.ZERO;
        if (!coupon.isActive()) return BigDecimal.ZERO;
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now()))
            return BigDecimal.ZERO;

        if (coupon.getType() == null) return BigDecimal.ZERO;

        Evaluator evaluator = evaluatorMap.get(coupon.getType());

        if (evaluator == null) {
            return BigDecimal.ZERO;
        }

        try {
            return Optional.ofNullable(evaluator.evaluate(coupon, cart))
                    .orElse(BigDecimal.ZERO);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Return map of couponId -> discount for all coupons that produce a discount > 0 for the cart.
     */

    public Map<String, BigDecimal> applicableCouponsForCart(CartDto cart) {
        List<Coupon> coupons = repo.findAll();

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Coupon coupon : coupons) {
            BigDecimal d = evaluateDiscountForCoupon(coupon, cart);
            if (d.compareTo(BigDecimal.ZERO) > 0) {
                result.put(coupon.getId(), d);
            }
        }
        return result;
    }

    /**
     * Apply the given coupon to the cart. Delegates to the Evaluator's apply(...) method.
     */
    public CartDto applyCouponToCart(Coupon coupon, CartDto cart) {
        cart.getItems().forEach(item -> item.setTotalDiscount(BigDecimal.ZERO));

        if(coupon == null)    return cart;
        if(!coupon.isActive())   return cart;
        if(coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())){
            return cart;
        }
        if(coupon.getType() == null)    return cart;

        Evaluator evaluator = evaluatorMap.get(coupon.getType());
        if(evaluator == null){
            return cart;
        }

        try {
            evaluator.apply(coupon,cart);
        } catch (Exception e) {
            //cart remains with zero discounts
        }
        return cart;
    }
}
