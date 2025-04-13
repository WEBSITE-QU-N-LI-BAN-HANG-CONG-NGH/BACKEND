package com.webanhang.team_project.service.seller;

import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.model.Product;
import org.springframework.data.domain.Page;

public interface ISellerProductService {
    Page<Product> getSellerProducts(Long sellerId, int page, int size, String search);
    Product createProduct(Long sellerId, CreateProductRequest request);
    Product getProductDetail(Long sellerId, Long productId);
    Product updateProduct(Long sellerId, Long productId, Product product);
    void deleteProduct(Long sellerId, Long productId);
    Product updateInventory(Long sellerId, Long productId, int quantity);
}
