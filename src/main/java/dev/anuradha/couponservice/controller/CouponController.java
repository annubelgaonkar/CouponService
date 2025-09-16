package dev.anuradha.couponservice.controller;

import dev.anuradha.couponservice.dto.CouponMapper;
import dev.anuradha.couponservice.dto.CouponRequestDto;
import dev.anuradha.couponservice.dto.CouponResponseDto;
import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<CouponResponseDto> create(@Valid @RequestBody CouponRequestDto couponRequestDto){
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
    public ResponseEntity<CouponResponseDto> update(@PathVariable String id,
                                                    @Valid @RequestBody CouponRequestDto coupon){

       Coupon updatedEntity = couponMapper.toEntity(coupon);

       return couponService.update(id, updatedEntity)
               .map(couponMapper::toResponse)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.notFound().build());
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
