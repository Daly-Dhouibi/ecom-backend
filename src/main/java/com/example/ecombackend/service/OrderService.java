package com.example.ecombackend.service;

import com.example.ecombackend.dto.OrderItemResponse;
import com.example.ecombackend.dto.OrderResponse;
import com.example.ecombackend.dto.OrderStatusRequest;
import com.example.ecombackend.entity.*;
import com.example.ecombackend.repository.CartRepository;
import com.example.ecombackend.repository.OrderRepository;
import com.example.ecombackend.repository.ProductRepository;
import com.example.ecombackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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



    public OrderResponse createOrderFromCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (user.getRole() == Role.ROLE_ADMIN) {
          //exception administrators are not allowed to place orders
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + username));

        if (cart.getCartItems() == null || cart.getCartItems() .isEmpty()) {
            //exception cannot create order from an empty cart
        }

        // 1. Validate
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() > product.getStockQuantity()) {
               // exception insufficient stock for product

            }
        }

        // 2. Init
        Order order = Order.builder()
                .customer(user)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Create OrderItems etc...
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            BigDecimal itemPrice = product.getPrice();
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(itemPrice)
                    .build();

            order.getItems().add(orderItem);

            BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            // 4. Decrease product stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // 5. Clear the cart
        cart.getCartItems().clear();
        cartRepository.save(cart);

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
          // exception access denied
        }

        return toOrderResponse(order);
    }

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
