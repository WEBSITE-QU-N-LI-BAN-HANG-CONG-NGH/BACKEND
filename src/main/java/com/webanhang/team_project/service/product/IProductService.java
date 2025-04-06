package com.webanhang.team_project.service.product;



import com.webanhang.team_project.dto.product.ProductDto;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.dto.product.request.AddProductRequest;
import com.webanhang.team_project.dto.product.request.UpdateProductRequest;

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

    List<ProductDto> getConvertedProducts(List<Product> products);

    ProductDto convertToDto(Product product);
}
