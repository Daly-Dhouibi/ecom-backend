package com.example.ecombackend.service;

import com.example.ecombackend.dto.CategoryRequest;
import com.example.ecombackend.dto.CategoryResponse;
import com.example.ecombackend.entity.Category;
import com.example.ecombackend.exception.BadRequestException;
import com.example.ecombackend.exception.ResourceNotFoundException;
import com.example.ecombackend.repository.CategoryRepository;
import com.example.ecombackend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService (CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toCategoryResponse(category);


    }
    public CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        if (categoryRepository.existsByName(categoryRequest.getName())) {
            throw new BadRequestException("Category with name '" + categoryRequest.getName() + "' already exists");
        }
        Category category = Category.builder()
                .name(categoryRequest.getName())
                .description(categoryRequest.getDescription())
                .build();
        Category savedCategory = categoryRepository.save(category);
        return toCategoryResponse(savedCategory);
    }
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // If changing name, ensure uniqueness
        if (!category.getName().equalsIgnoreCase(request.getName().trim())
                && categoryRepository.existsByName(request.getName().trim())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        return toCategoryResponse(updatedCategory);
    }
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!productRepository.findByCategoryId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete category because products are assigned to it");
        }
        categoryRepository.delete(category);
    }

}
