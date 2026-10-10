package com.example.ecommerce.dto;

import com.example.ecommerce.dto.CartItemResponse;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long id;
    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();
    private BigDecimal totalAmount;
    private Integer totalItems;
}
