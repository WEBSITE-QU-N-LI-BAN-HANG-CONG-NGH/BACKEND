package com.webanhang.team_project.service.product;



import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.product.AddProductRequest;
import com.webanhang.team_project.dto.product.UpdateProductRequest;
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
    
    // Thêm các phương thức cần thiết cho controller
    List<Product> getAllProducts();
    Product getProductById(int productId);
    List<Product> getProductsByCategory(String category);
    List<Product> getProductsByName(String name);
    List<Product> getProductsByBrand(String brand);
    List<Product> getProductsByBrandAndName(String brand, String name);
    List<Product> getProductsByCategoryAndBrand(String category, String brand);
    List<ProductDTO> getConvertedProducts(List<Product> products);
    Product addProduct(AddProductRequest request);
    Product updateProduct(UpdateProductRequest request, int productId);
    void deleteProduct(int productId);
}
