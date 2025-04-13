package com.webanhang.team_project.service.product;



import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.model.Product;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IProductService {

    ProductDTO convertToDto(Product product);

    Product createProduct(CreateProductRequest req);

    String deleteProduct(Long productId);

    Product updateProduct(Long productId, Product product);

    Product findProductById(Long id);

    List<Product> findProductByCategory(String category);

    Page<Product> findAllProductsByFilter(String category, List<String> colors, List<String> sizes,
                                                 Integer minPrice, Integer maxPrice, Integer minDiscount, String sort,
                                                 String stock, Integer pageNumber, Integer pageSize);

    List<Product> findAllProducts();

    List<Product> searchProducts(String keyword);

    List<Product> getFeaturedProducts();
}
