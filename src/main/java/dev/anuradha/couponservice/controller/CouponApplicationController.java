package dev.anuradha.couponservice.controller;

import dev.anuradha.couponservice.dto.CartDto;
import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CouponApplicationController {

    private final CouponService couponService;

    @PostMapping("/applicable-coupons")
    public ResponseEntity<?> applicableCoupons(@RequestBody CartDto cartDto){
        Map<String, BigDecimal> map = couponService.applicableCouponsForCart(cartDto);

        List<Map<String,Object>> resp = new ArrayList<>();
        for(Map.Entry<String, BigDecimal> entry : map.entrySet()){
            couponService.findById(entry.getKey()).ifPresent(coupon ->{
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("coupon_id", coupon.getId());
                item.put("code", coupon.getCode());
                item.put("type", coupon.getType());
                item.put("discount", entry.getValue());
                resp.add(item);
            });
        }
        return ResponseEntity.ok(Collections.singletonMap("applicable_coupons",resp));
    }

    @PostMapping("/apply-coupon/{id}")
    public ResponseEntity<?> applyCoupon(@PathVariable String id,
                                         @RequestBody CartDto cartDto){
        Optional<Coupon> optionalCoupon = couponService.findById(id);

        if(optionalCoupon.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        Coupon coupon = optionalCoupon.get();
        CartDto updated = couponService.applyCouponToCart(coupon, cartDto);

        // compute totals
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal totalPrice = updated.getItems().stream()
                .map(i -> i.getPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = updated.getItems().stream()
                .map(i -> i.getTotalDiscount() == null ? BigDecimal.ZERO : i.getTotalDiscount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("items", updated.getItems());
        result.put("total_price", totalPrice);
        result.put("total_discount", totalDiscount);
        result.put("final_price", totalPrice.subtract(totalDiscount));
        return ResponseEntity.ok(Collections.singletonMap("updated_cart", result));
    }
}
