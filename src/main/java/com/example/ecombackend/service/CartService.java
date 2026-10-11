package com.example.ecombackend.service;

import com.example.ecombackend.dto.CartItemRequest;
import com.example.ecombackend.dto.CartItemResponse;
import com.example.ecombackend.dto.CartItemUpdateRequest;
import com.example.ecombackend.entity.Cart;
import com.example.ecombackend.entity.CartItem;
import com.example.ecombackend.entity.Product;
import com.example.ecombackend.entity.User;
import com.example.ecombackend.dto.CartResponse;
import com.example.ecombackend.repository.CartItemRepository;
import com.example.ecombackend.repository.CartRepository;
import com.example.ecombackend.repository.ProductRepository;
import com.example.ecombackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }
    public Cart getOrCreateCartForUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .cartItems(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }
    public CartResponse getCartForUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        return toCartResponse(cart);
    }
    // cart mapper
    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;

        if (cart.getCartItems() != null) {
            for (CartItem item : cart.getCartItems()) {
                BigDecimal subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(subtotal);
                totalItems += item.getQuantity();

                itemResponses.add(CartItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productPrice(item.getProduct().getPrice())
                        .productImageUrl(item.getProduct().getImageUrl())
                        .quantity(item.getQuantity())
                        .subtotal(subtotal)
                        .build());
            }
        }

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
    }
    public CartResponse addItemToCart(String username,
                                      CartItemRequest request) {
        if (request.getQuantity() <= 0) {
           //exception invalid quantity
        }

        User user = getUserByUsername(username);
        if (user.getRole() == com.example.ecombackend.entity.Role.ROLE_ADMIN) {
           //exception can't order
        }
        Cart cart = getOrCreateCartForUser(user);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        Optional<CartItem> existingItemOpt = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        int targetQuantity = request.getQuantity();
        if (existingItemOpt.isPresent()) {
            targetQuantity += existingItemOpt.get().getQuantity();
        }

        if (targetQuantity > product.getStockQuantity()) {
            //exception
        }

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(targetQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getCartItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        Cart updatedCart = cartRepository.save(cart);
        return toCartResponse(updatedCart);
    }
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
    public CartResponse updateCartItemQuantity(String username, Long cartItemId,
                                               CartItemUpdateRequest request) {
        if (request.getQuantity() <= 0) {
            //exception
        }

        User user = getUserByUsername(username);
        Cart cart = getOrCreateCartForUser(user);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            //exception
        }

        Product product = cartItem.getProduct();
        if (request.getQuantity() > product.getStockQuantity()) {
            //exception
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        Cart updatedCart = cartRepository.findById(cart.getId()).orElse(cart);
        return toCartResponse(updatedCart);
    }
    public CartResponse removeCartItem(String username, Long cartItemId) {
        User user = getUserByUsername(username);
        Cart cart = getOrCreateCartForUser(user);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            //exception
        }

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        Cart updatedCart = cartRepository.save(cart);
        return toCartResponse(updatedCart);
    }
}
