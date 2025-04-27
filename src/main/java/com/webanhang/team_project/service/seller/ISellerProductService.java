package com.webanhang.team_project.service.seller;

import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.model.Product;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ISellerProductService {
    ProductDTO createProduct(CreateProductRequest request);

    ProductDTO updateProduct(Long productId, Product product);

    void deleteProduct(Long productId);

    List<ProductDTO> getSellerProducts(Long sellerId);

    ProductDTO getProductDetail(Long productId);

    Map<String, Object> getProductStatOfSeller(Long sellerId);

    List<ProductDTO> createMultipleProducts(List<CreateProductRequest> requests);
}
