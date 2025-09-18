package dev.anuradha.couponservice.config;

import dev.anuradha.couponservice.model.CouponType;
import dev.anuradha.couponservice.service.BxGyEvaluator;
import dev.anuradha.couponservice.service.CartWiseEvaluator;
import dev.anuradha.couponservice.service.Evaluator;
import dev.anuradha.couponservice.service.ProductWiseEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class EvaluatorConfig {


    @Bean
    public Map<CouponType, Evaluator> evaluatorMap(CartWiseEvaluator cartEvaluator,
                                                   ProductWiseEvaluator productEvaluator,
                                                   BxGyEvaluator bxGyEvaluator) {
        Map<CouponType, Evaluator> map = new EnumMap<>(CouponType.class);
        map.put(CouponType.CART, cartEvaluator);
        map.put(CouponType.PRODUCT, productEvaluator);
        map.put(CouponType.BXGY, bxGyEvaluator);
        return map;
    }
}
