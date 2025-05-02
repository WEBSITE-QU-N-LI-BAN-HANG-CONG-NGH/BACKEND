package com.webanhang.team_project.controller.common;

import com.webanhang.team_project.dto.product.FilterProduct;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/products")
public class ProductController {

    private final IProductService productService;

    @GetMapping("/")
    public ResponseEntity<List<ProductDTO>> findProductsByFilter(
            @RequestParam(required = false) String topLevelCategory,
            @RequestParam(required = false) String secondLevelCategory,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword
    ) {
        // Create FilterProduct object from request parameters
        FilterProduct filterProduct = new FilterProduct();
        filterProduct.setTopLevelCategory(topLevelCategory);
        filterProduct.setSecondLevelCategory(secondLevelCategory);
        filterProduct.setColor(color);
        filterProduct.setMinPrice(minPrice);
        filterProduct.setMaxPrice(maxPrice);
        filterProduct.setSort(sort);
        filterProduct.setKeyword(keyword);

        // Log the filter request for debugging
        System.out.println("Processing filter request with parameters: " + filterProduct);

        // Get filtered products
        List<Product> filteredProducts = productService.findAllProductsByFilter(filterProduct);

        // Convert to DTOs
        List<ProductDTO> productDTOs = filteredProducts.stream()
                .map(ProductDTO::new)
                .toList();

        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }

    @GetMapping("/id/{productId}")
    public ResponseEntity<ApiResponse> findProductById(@PathVariable Long productId) {
        Product product = productService.findProductById(productId);
        ProductDTO productDTO = new ProductDTO(product);
        return ResponseEntity.ok(ApiResponse.success(productDTO, "Get product by id successfully"));
    }


    @GetMapping("/all")
    public ResponseEntity<List<ProductDTO>> getAllProductsWithoutFilter() {
        List<Product> products = productService.findAllProducts();
        List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::new)
                .toList();
        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }
}