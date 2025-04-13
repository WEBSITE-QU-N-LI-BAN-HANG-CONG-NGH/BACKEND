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

    public Page<Product> findAllProductsByFilter(String category, List<String> colors, List<String> sizes,
                                                 Integer minPrice, Integer maxPrice, Integer minDiscount, String sort,
                                                 String stock, Integer pageNumber, Integer pageSize) ;

    public List<Product> findAllProducts() ;

    public List<Product> searchProducts(String keyword);

    public List<Product> getFeaturedProducts();
    
    public List<Map<String, Object>> getTopSellingProducts(int limit);
    
    public Map<String, Object> getRevenueByCateogry();
}
