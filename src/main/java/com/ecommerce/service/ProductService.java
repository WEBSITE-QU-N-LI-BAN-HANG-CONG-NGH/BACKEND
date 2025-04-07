package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Product;
import com.ecommerce.request.CreateProductRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    public Product createProduct(CreateProductRequest req) throws GlobalExceptionHandler;

    public String deleteProduct(Long productId) throws GlobalExceptionHandler;

    public Product updateProduct(Long productId, Product product) throws GlobalExceptionHandler;

    public Product findProductById(Long id) throws GlobalExceptionHandler;

    public List<Product> findProductByCategory(String category) throws GlobalExceptionHandler;

    public Page<Product> findAllProductsByFilter(String category, List<String> colors, List<String> sizes,
                                         Integer minPrice, Integer maxPrice, Integer minDiscount, String sort,
                                         String stock, Integer pageNumber, Integer pageSize) throws GlobalExceptionHandler;

    public List<Product> findAllProducts() throws GlobalExceptionHandler;

    public List<Product> searchProducts(String keyword) throws GlobalExceptionHandler;

    public List<Product> getFeaturedProducts() throws GlobalExceptionHandler;
}
