package dev.anuradha.couponservice.dto;

import dev.anuradha.couponservice.model.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public Coupon toEntity(CouponRequestDto req){
        Coupon coupon = new Coupon();
        coupon.setCode(req.getCode());
        coupon.setType(req.getType());
        coupon.setDetails(req.getDetails());
        coupon.setActive(req.isActive());
        return coupon;
    }

    public CouponResponseDto toResponse(Coupon coupon){
        CouponResponseDto responseDto = new CouponResponseDto();
        responseDto.setId(coupon.getId());
        responseDto.setCode(responseDto.getCode());
        responseDto.setType(coupon.getType());
        responseDto.setDetails(coupon.getDetails());
        responseDto.setActive(coupon.isActive());
        responseDto.setExpiresAt(coupon.getExpiresAt());
        responseDto.setCreatedAt(coupon.getCreatedAt());
        responseDto.setUpdatedAt(coupon.getUpdatedAt());
        return responseDto;
    }
}
