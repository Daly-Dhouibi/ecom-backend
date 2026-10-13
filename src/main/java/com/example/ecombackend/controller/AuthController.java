package com.example.ecombackend.controller;

import com.example.ecombackend.dto.RegisterRequest;
import com.example.ecombackend.dto.UserProfileUpdateRequest;
import com.example.ecombackend.dto.UserResponse;
import com.example.ecombackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        UserResponse response = authService.getCurrentUser(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(Principal principal,
                                                          @Valid @RequestBody UserProfileUpdateRequest request) {
        UserResponse response = authService.updateCurrentUser(principal.getName(), request);
        return ResponseEntity.ok(response);
    }
}
