package dev.anuradha.couponservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.anuradha.couponservice.dto.*;
import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.model.CouponType;
import dev.anuradha.couponservice.repositories.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor
class CouponServiceTest {

    @Mock
    CouponRepository repo;

    @InjectMocks
    CouponService service;

    private final ObjectMapper objectMapper;

    @BeforeEach
    void setUp(){
        service = new CouponService(repo, objectMapper);
    }

    @Test
    void testEvaluateCartWise_percent() throws Exception{
        CartDto cartDto = new CartDto(List.of(
                new CartItemDto(1L, 2, BigDecimal.valueOf(100),null),
                new CartItemDto(2L, 1, BigDecimal.valueOf(50),null)
        ));

        CartWiseDetailsDto detailsDto = new CartWiseDetailsDto(
                BigDecimal.valueOf(100),
                "PERCENT", BigDecimal.valueOf(10)
        );

        Coupon coupon = new Coupon();
        coupon.setType(CouponType.CART);
        coupon.setDetails(objectMapper.writeValueAsString(detailsDto));
        coupon.setActive(true);

        var discount = service.evaluateDiscountForCoupon(coupon, cartDto);
        assertEquals(0, discount.compareTo(BigDecimal.valueOf(25)));
    }

    @Test
    void testEvaluateProductWise_percent() throws Exception {
        CartDto cart = new CartDto(List.of(
                new CartItemDto(1L, 3, BigDecimal.valueOf(30), null),
                new CartItemDto(2L, 1, BigDecimal.valueOf(100), null)
        ));

        ProductWiseDetailsDto details = new ProductWiseDetailsDto(1L,
                "PERCENT", BigDecimal.valueOf(20)
        );

        Coupon c = new Coupon();
        c.setType(CouponType.PRODUCT);
        c.setDetails(objectMapper.writeValueAsString(details));
        c.setActive(true);

        var discount = service.evaluateDiscountForCoupon(c, cart);

        assertEquals(0, discount.compareTo(BigDecimal.valueOf(18)));
    }

    @Test
    void testEvaluateBxGy_basic() throws Exception {
        CartDto cart = new CartDto(List.of(
                new CartItemDto(1L, 4, BigDecimal.valueOf(50), null), // buy product
                new CartItemDto(3L, 1, BigDecimal.valueOf(20), null)  // get product
        ));

        BxGyDetailsDto bxGyDetailsDto = new BxGyDetailsDto(
                List.of(new BxGyDetailsDto.BuyProduct(1L, 2)),
                List.of(new BxGyDetailsDto.GetProduct(3L, 1)),
                2
        );

        Coupon c = new Coupon();
        c.setType(CouponType.BXGY);
        c.setDetails(objectMapper.writeValueAsString(bxGyDetailsDto));
        c.setActive(true);

        var discount = service.evaluateDiscountForCoupon(c, cart);
        // buy 4 of product 1 -> floor(4/2)=2 reps, repetitionLimit 2 => 2 free units of product 3 but only 1 present => 1*20 = 20
        assertEquals(0, discount.compareTo(BigDecimal.valueOf(20)));
    }

}