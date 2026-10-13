package com.example.ecombackend.controller;

import com.example.ecombackend.dto.CartItemRequest;
import com.example.ecombackend.dto.CartItemUpdateRequest;
import com.example.ecombackend.entity.User;
import com.example.ecombackend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import com.example.ecombackend.dto.CartResponse;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Principal principal) {
        CartResponse cart = cartService.getCartForUsername(principal.getName());
        return ResponseEntity.ok(cart);
    }
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(Principal principal,
                                                      @Valid @RequestBody CartItemRequest request){
        return ResponseEntity.ok(cartService.addItemToCart(principal.getName(), request));
    }
    @PutMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateItemQuantity(Principal principal,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody CartItemUpdateRequest request) {
        CartResponse cart = cartService.updateCartItemQuantity(principal.getName(), id, request);
        return ResponseEntity.ok(cart);
    }
    @DeleteMapping("/items/{id}")
    public ResponseEntity<CartResponse> removeItemFromCart(Principal principal,
                                                           @PathVariable Long id) {
        CartResponse cart = cartService.removeCartItem(principal.getName(), id);
        return ResponseEntity.ok(cart);
    }
}
