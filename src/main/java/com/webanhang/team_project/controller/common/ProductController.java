package com.webanhang.team_project.controller.common;


import com.webanhang.team_project.dto.product.ProductDto;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.dto.product.request.AddProductRequest;
import com.webanhang.team_project.dto.product.request.UpdateProductRequest;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/products")
public class ProductController {
    private final IProductService productService;

    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        List<ProductDto> convertedProducts = productService.getConvertedProducts(products);
        return ResponseEntity.ok(ApiResponse.success(convertedProducts, "Get Success"));
    }

    @GetMapping("/product/{productId}/product")
    public ResponseEntity<ApiResponse> getProductById(@PathVariable int productId) {
        Product product = productService.getProductById(productId);
        ProductDto productDto = productService.convertToDto(product);
        return ResponseEntity.ok(ApiResponse.success(productDto, "Success !!"));
    }

    @GetMapping("/product/{category}/product")
    public ResponseEntity<ApiResponse> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productService.getProductsByCategory(category);
        List<ProductDto> productDtos = productService.getConvertedProducts(products);
        return ResponseEntity.ok(ApiResponse.success(productDtos, "Success !!"));
    }

    @GetMapping("/product/{name}/product")
    public ResponseEntity<ApiResponse> getProductsByName(@PathVariable String name) {
        List<Product> products = productService.getProductsByName(name);
        List<ProductDto> productDtos = productService.getConvertedProducts(products);
        return ResponseEntity.ok(ApiResponse.success(productDtos, "Success !!"));
    }

    @GetMapping("/product/{brand}/product")
    public ResponseEntity<ApiResponse> getProductsByBrand(@PathVariable String brand) {
        List<Product> products = productService.getProductsByBrand(brand);
        List<ProductDto> productDtos = productService.getConvertedProducts(products);
        return ResponseEntity.ok(ApiResponse.success(productDtos, "Success !!"));
    }

    @GetMapping("/product/by-brand-and-name")
    public ResponseEntity<ApiResponse> getProductsByBrandAndName(@RequestParam String brand, @RequestParam String name) {
        List<Product> products = productService.getProductsByBrandAndName(brand, name);
        List<ProductDto> productDtos = productService.getConvertedProducts(products);
        return ResponseEntity.ok(ApiResponse.success(productDtos, "Success !!"));
    }

    @GetMapping("/product/by-category-and-name")
    public ResponseEntity<ApiResponse> getProductsByCategoryAndName(@RequestParam String category, @RequestParam String brand) {
        List<Product> products = productService.getProductsByCategoryAndBrand(category, brand);
        List<ProductDto> productDtos = productService.getConvertedProducts(products);
        return ResponseEntity.ok(ApiResponse.success(productDtos, "Success !!"));
    }

    @PostMapping("/product/add")
    public ResponseEntity<ApiResponse> addProduct(@RequestBody AddProductRequest request) {
        Product product = productService.addProduct(request);
        ProductDto productDto = productService.convertToDto(product);
        return ResponseEntity.ok(ApiResponse.success(productDto, "Add product success !!"));
    }

    @PutMapping("/product/{productId}/update")
    public ResponseEntity<ApiResponse> updateProduct(@RequestBody UpdateProductRequest request, @PathVariable int productId) {
        Product product = productService.updateProduct(request, productId);
        ProductDto productDto = productService.convertToDto(product);
        return ResponseEntity.ok(ApiResponse.success(productDto, "Update product Success !!"));
    }

    @DeleteMapping("/product/delete/{productId}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable int productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(null, "Delete product Success !!"));
    }
}
