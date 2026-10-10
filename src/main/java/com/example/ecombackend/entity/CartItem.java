package com.example.ecombackend.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "cart_items")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter

public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id",nullable = false)
    private Cart cart;

    // Join product

    @Column(nullable = false)
    private Integer quantity;
}
