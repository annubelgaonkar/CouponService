package dev.anuradha.couponservice.service;

import dev.anuradha.couponservice.dto.CartDto;
import dev.anuradha.couponservice.model.Coupon;

import java.math.BigDecimal;

public interface Evaluator {
    BigDecimal evaluate(Coupon coupon, CartDto cartDto);
    void apply(Coupon coupon, CartDto cartDto);
}
