package dev.anuradha.couponservice.dto;

import dev.anuradha.couponservice.model.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public Coupon toEntity(CouponRequestDto req) {
        if (req == null) return null;
        Coupon coupon = new Coupon();
        coupon.setCode(req.getCode());
        coupon.setType(req.getType());
        coupon.setDetails(req.getDetails());
        coupon.setActive(req.isActive());
        coupon.setExpiresAt(req.getExpiresAt());
        return coupon;
    }

    public CouponResponseDto toResponse(Coupon c) {
        if (c == null) return null;
        CouponResponseDto responseDto = new CouponResponseDto();
        responseDto.setId(c.getId());
        responseDto.setCode(c.getCode());               // <- important: set code
        responseDto.setType(c.getType());
        responseDto.setDetails(c.getDetails());
        responseDto.setActive(c.isActive());
        responseDto.setExpiresAt(c.getExpiresAt());
        responseDto.setCreatedAt(c.getCreatedAt());
        responseDto.setUpdatedAt(c.getUpdatedAt());
        return responseDto;
    }
}
