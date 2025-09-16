package dev.anuradha.couponservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.anuradha.couponservice.dto.CouponMapper;
import dev.anuradha.couponservice.dto.CouponRequestDto;
import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.model.CouponType;
import dev.anuradha.couponservice.service.CouponService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.nullable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CouponController.class)
@Import(CouponMapper.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CouponService couponService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Test
    void createCoupon_returnsCreated() throws Exception {
        CouponRequestDto req = new CouponRequestDto("CART10", CouponType.CART,
                "{\"threshold\":100,\"discountType\":\"PERCENT\",\"discountValue\":10}",
                true, null);

        // Use mapper to create the entity the controller would create
        Coupon entityFromReq = couponMapper.toEntity(req);
        entityFromReq.setId("id-1"); // simulate repo-generated id

        // stub the service to return the saved entity
        when(couponService.create(any(Coupon.class))).thenReturn(entityFromReq);

        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CART10"))
                .andExpect(jsonPath("$.id").value("id-1"));
    }

}