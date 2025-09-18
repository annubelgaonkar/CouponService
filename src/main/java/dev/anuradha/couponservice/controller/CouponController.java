package dev.anuradha.couponservice.controller;

import dev.anuradha.couponservice.dto.CouponMapper;
import dev.anuradha.couponservice.dto.CouponRequestDto;
import dev.anuradha.couponservice.dto.CouponResponseDto;
import dev.anuradha.couponservice.dto.UpdateCouponDto;
import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.model.CouponType;
import dev.anuradha.couponservice.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponMapper couponMapper;

    //create a coupon
    @PostMapping
    public ResponseEntity<CouponResponseDto> create(@RequestBody CouponRequestDto couponRequestDto){
        //map the requestDto to entity
        Coupon entity = couponMapper.toEntity(couponRequestDto);

        //save
        Coupon createdCoupon = couponService.create(entity);

        //map entity to responseDto
        CouponResponseDto response = couponMapper.toResponse(createdCoupon);


        return ResponseEntity.created(URI.create("/api/coupons/" + createdCoupon.getId()))
                .body(response);
    }


    //Get all coupons list
    @GetMapping
    public ResponseEntity<List<CouponResponseDto>> listAll(){
        List<Coupon> coupons = couponService.listAll();
        List<CouponResponseDto> responses = coupons.stream()
                .map(couponMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }


    //get coupon by id
    @GetMapping("/{id}")
    public ResponseEntity<CouponResponseDto> getById(@PathVariable String id){
        return couponService.findById(id)
                .map(couponMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //update coupon by id
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCoupon(@PathVariable String id,
                                          @RequestBody UpdateCouponDto couponDto){

        Coupon updated = new Coupon();
        if (couponDto.getCode() != null) updated.setCode(couponDto.getCode());
        if (couponDto.getType() != null) {
            try {
                updated.setType(CouponType.valueOf(couponDto.getType()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid coupon type: " + couponDto.getType());
            }
        }
        if (couponDto.getDetails() != null) updated.setDetails(couponDto.getDetails());
        if (couponDto.getActive() != null) updated.setActive(couponDto.getActive());
        if (couponDto.getExpiresAt() != null) updated.setExpiresAt(couponDto.getExpiresAt());

        return couponService.update(id, couponDto)
                .map(c -> ResponseEntity.ok(couponMapper
                        .toResponse(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    //delete coupon by id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id){

        //first check if the coupon to be deleted exists
        if(couponService.findById(id).isEmpty()){
            return ResponseEntity.notFound().build();
        }
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
