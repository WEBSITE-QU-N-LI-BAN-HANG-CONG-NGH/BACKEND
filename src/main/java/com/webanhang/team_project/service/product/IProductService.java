package com.webanhang.team_project.service.product;



import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.model.Product;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface IProductService {
    public Product createProduct(CreateProductRequest req) ;

    public String deleteProduct(Long productId) ;

    public Product updateProduct(Long productId, Product product) ;

    public Product findProductById(Long id) ;

    public List<Product> findProductByCategory(String category) ;

    List<Product> findAllProductsByFilter(
            List<String> colors,
            Integer minPrice,
            Integer maxPrice,
            Integer minDiscount,
            String sort);

    public List<Product> findAllProducts() ;

    public List<Product> searchProducts(String keyword);

    public List<Map<String, Object>> getTopSellingProducts(int limit);
    
    public Map<String, Object> getRevenueByCateogry();

    public List<Product> findByCategoryTopAndSecond(String topCategory, String secondCategory);
}
