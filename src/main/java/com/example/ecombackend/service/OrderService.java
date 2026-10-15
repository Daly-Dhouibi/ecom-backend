package com.example.ecombackend.service;

import com.example.ecombackend.dto.OrderItemResponse;
import com.example.ecombackend.dto.OrderResponse;
import com.example.ecombackend.dto.OrderStatusRequest;
import com.example.ecombackend.entity.*;
import com.example.ecombackend.exception.BadRequestException;
import com.example.ecombackend.exception.InsufficientStockException;
import com.example.ecombackend.exception.ResourceNotFoundException;
import com.example.ecombackend.repository.CartRepository;
import com.example.ecombackend.repository.OrderRepository;
import com.example.ecombackend.repository.ProductRepository;
import com.example.ecombackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional
    public OrderResponse createOrderFromCart(String username) {

        // 1. Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username
                ));

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new BadRequestException(
                    "Administrators are not permitted to place orders"
            );
        }

        // 2. Find cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: " + username
                ));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new BadRequestException(
                    "Cannot create order from an empty cart"
            );
        }

        // 3. Initialize order
        Order order = Order.builder()
                .customer(user)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 4. Create order items and atomically decrease stock
        for (CartItem cartItem : cart.getCartItems()) {

            Product product = cartItem.getProduct();
            Integer quantity = cartItem.getQuantity();

            // Atomically check and decrease stock
            int updated = productRepository.decrementStockIfAvailable(
                    product.getId(),
                    quantity
            );

            if (updated == 0) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '"
                                + product.getName() + "'."
                );
            }

            // Create order item
            BigDecimal itemPrice = product.getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .price(itemPrice)
                    .build();

            order.getItems().add(orderItem);

            // Calculate total
            BigDecimal lineTotal = itemPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );

            totalAmount = totalAmount.add(lineTotal);
        }

        // 5. Save order
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // 6. Clear cart
        cart.getCartItems().clear();
        cartRepository.save(cart);

        // 7. Return response
        return toOrderResponse(savedOrder);
    }

    public List<OrderResponse> getOrdersForUser(String username) {
        return orderRepository.findByCustomerUsernameOrderByCreatedDateDesc(username).stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }



    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByCreatedDateDesc().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }



    public OrderResponse getOrderById(Long orderId, String username, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isAdmin && !order.getCustomer().getUsername().equals(username)) {
            throw new BadRequestException("Access denied: You can only view your own orders");
        }

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // If transitioned to cancelled -> restore product stock
        if (request.getStatus() == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(request.getStatus());
        Order updatedOrder = orderRepository.save(order);
        return toOrderResponse(updatedOrder);
    }



    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                itemResponses.add(OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProduct() != null ? item.getProduct().getName() : "Deleted Product")
                        .productImageUrl(item.getProduct() != null ? item.getProduct().getImageUrl() : null)
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(subtotal)
                        .build());
            }
        }

        String fullName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerUsername(order.getCustomer().getUsername())
                .customerFullName(fullName)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdDate(order.getCreatedDate())
                .items(itemResponses)
                .build();
    }
}
