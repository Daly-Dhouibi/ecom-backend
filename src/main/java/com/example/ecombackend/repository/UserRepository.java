package com.example.ecombackend.repository;

import com.example.ecombackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String firstName);
    boolean existsByUsername(String username);
}
