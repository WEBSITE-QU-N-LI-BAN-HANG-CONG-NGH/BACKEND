package com.webanhang.team_project.service.product;



import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.product.request.CreateProductRequest;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.dto.product.request.AddProductRequest;
import com.webanhang.team_project.dto.product.request.UpdateProductRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IProductService {
    Product addProduct(AddProductRequest request);
    Product updateProduct(UpdateProductRequest request, int productId);
    void deleteProduct(int productId);
    Product getProductById(int productId);


    List<Product> getAllProducts();
    List<Product> getProductsByCategoryAndBrand(String category, String brand);
    List<Product> getProductsByBrandAndName(String name, String brand);
    List<Product> getProductsByName(String name);
    List<Product> getProductsByBrand(String brand);
    List<Product> getProductsByCategory(String category);

    List<ProductDTO> getConvertedProducts(List<Product> products);

    ProductDTO convertToDto(Product product);

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
