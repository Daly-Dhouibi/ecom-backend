package com.example.ecombackend.controller;

import com.example.ecombackend.dto.OrderResponse;
import com.example.ecombackend.dto.OrderStatusRequest;
import com.example.ecombackend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(Authentication authentication) {
        OrderResponse order = orderService.createOrderFromCart(authentication.getName());
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority
                        -> grantedAuthority.getAuthority()
                        .equals("ROLE_ADMIN"));

        List<OrderResponse> orders;
        if (isAdmin) {
            orders = orderService.getAllOrdersForAdmin();
        } else {
            orders = orderService.getOrdersForUser(authentication.getName());
        }

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id,
                                                      Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority
                        -> grantedAuthority.getAuthority()
                        .equals("ROLE_ADMIN"));

        OrderResponse order = orderService.getOrderById(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                           @Valid @RequestBody OrderStatusRequest request) {
        OrderResponse updated = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(updated);
    }
}
