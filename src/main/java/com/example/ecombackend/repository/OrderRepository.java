package com.example.ecombackend.repository;

import com.example.ecombackend.entity.Order;
import com.example.ecombackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerUsernameOrderByCreatedDateDesc(String username);
    List<Order> findAllByOrderByCreatedDateDesc();
}
