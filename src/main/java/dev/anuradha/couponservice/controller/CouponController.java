package dev.anuradha.couponservice.controller;

import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    //create a coupon
    @PostMapping
    public ResponseEntity<Coupon> create(@Valid @RequestBody Coupon coupon){
        Coupon createdCoupon = couponService.create(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCoupon);
    }

    //Get all coupons
    @GetMapping
    public ResponseEntity<List<Coupon>> list(){
        return ResponseEntity.ok(couponService.couponList());
    }

    //get by id
    @GetMapping("/{id}")
    public ResponseEntity<Coupon> get(@PathVariable String id){
        return couponService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    //update by id
    @PutMapping("/{id}")
    public ResponseEntity<Coupon> update(@PathVariable String id,
                                         @Valid @RequestBody Coupon coupon){
        return couponService.update(id, coupon)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    //delete by id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id){
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
