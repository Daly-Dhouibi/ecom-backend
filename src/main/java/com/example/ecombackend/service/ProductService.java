package com.example.ecombackend.service;

import com.example.ecombackend.dto.ProductRequest;
import com.example.ecombackend.dto.ProductResponse;
import com.example.ecombackend.entity.Category;
import com.example.ecombackend.entity.Product;
import com.example.ecombackend.repository.CategoryRepository;
import com.example.ecombackend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    ProductService(ProductRepository productRepository,
                   CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResponse> getAllProducts(Long categoryId, String search) {
        List<Product> products;
        if (categoryId != null && search != null && !search.trim().isEmpty()) {
            products = productRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, search.trim());
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (search != null && !search.trim().isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search.trim(), search.trim());
        } else {
            products = productRepository.findAllByOrderByCreatedDateDesc();
        }
        return products.stream()
                .map(this::toProductResponse)
                .toList();
    }
    private ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdDate(product.getCreatedDate())
                .updatedDate(product.getUpdatedDate())
                .build();
    }
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::toProductResponse)
                .orElse(null);
    }
    public ProductResponse createProduct(ProductRequest productRequest) {
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));

        Product product = Product.builder()
                .name(productRequest.getName().trim())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .stockQuantity(productRequest.getStockQuantity())
                .imageUrl(productRequest.getImageUrl())
                .category(category)
                .build();

        Product savedProduct = productRepository.save(product);
        return toProductResponse(savedProduct);
    }
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        return toProductResponse(updatedProduct);
    }
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }
}
