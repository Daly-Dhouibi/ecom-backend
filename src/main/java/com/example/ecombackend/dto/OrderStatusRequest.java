package com.example.ecombackend.dto;

import com.example.ecombackend.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}