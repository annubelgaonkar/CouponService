package dev.anuradha.couponservice.dto;

import lombok.*;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BxGyDetailsDto {

    private List<BuyProduct> buyProducts;
    private List<GetProduct> getProducts;
    private Integer repetitionLimit;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BuyProduct{
        private Long productId;
        private Integer quantity;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GetProduct{
        private Long productId;
        private Integer quantity;
    }

}
