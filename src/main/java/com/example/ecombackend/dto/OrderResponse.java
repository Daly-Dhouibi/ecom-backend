package com.example.ecombackend.dto;

import com.example.ecombackend.entity.OrderStatus;
import com.example.ecombackend.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String customerUsername;
    private String customerFullName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdDate;
    private List<OrderItemResponse> items = new ArrayList<>();
}
