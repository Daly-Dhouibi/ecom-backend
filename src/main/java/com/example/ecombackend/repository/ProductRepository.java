package com.example.ecombackend.repository;

import com.example.ecombackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String name);
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
    List<Product> findAllByOrderByCreatedDateDesc();
    @Modifying
    @Query("""
    UPDATE Product p
    SET p.stockQuantity = p.stockQuantity - :qty
    WHERE p.id = :id
    AND p.stockQuantity >= :qty
    """)
    int decrementStockIfAvailable(
            @Param("id") Long id,
            @Param("qty") Integer qty
    );
}
