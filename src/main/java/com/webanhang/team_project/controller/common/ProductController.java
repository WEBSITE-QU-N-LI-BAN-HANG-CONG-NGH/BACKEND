package com.webanhang.team_project.controller.common;

import com.webanhang.team_project.dto.product.FilterProduct;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/products")
public class ProductController {

    private final IProductService productService;
//    private CategoryRepository categoryRepository;

    @GetMapping("/") // Endpoint vẫn là /api/v1/products/
    public ResponseEntity<List<ProductDTO>> findProductsByFilter(
            // Sử dụng @RequestParam thay vì @RequestBody
            @RequestParam(required = false) String topLevelCategory,
            @RequestParam(required = false) String secondLevelCategory,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer minPrice, // Nên dùng kiểu số nếu có thể
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String sort
            // Bỏ qua page/size nếu backend không phân trang ở đây
    ) {
        // Tạo đối tượng FilterProduct từ các RequestParam
        FilterProduct filterProduct = new FilterProduct();
        filterProduct.setTopLevelCategory(topLevelCategory);
        filterProduct.setSecondLevelCategory(secondLevelCategory);
        filterProduct.setColor(color);
        filterProduct.setMinPrice(minPrice); // Gán trực tiếp kiểu số
        filterProduct.setMaxPrice(maxPrice);
        filterProduct.setSort(sort);

        System.out.println("Received Filter Request (Query Params): " + filterProduct); // Log để kiểm tra

        List<Product> res = productService.findAllProductsByFilter(filterProduct);

        List<ProductDTO> productDTOs = res.stream()
                .map(ProductDTO::new)
                .toList();

        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }

    @GetMapping("/id/{productId}")
    public ResponseEntity<ApiResponse> findProductById(@PathVariable Long productId) {
        Product res = productService.findProductById(productId);
        ProductDTO productDTO = new ProductDTO(res);
        return ResponseEntity.ok(ApiResponse.success(productDTO, "Get product by id successfully"));
    }

    @GetMapping("/{topCategoryName}/{secondCategoryName}")
    public ResponseEntity<List<ProductDTO>> findProductByPath(
            @PathVariable String topCategoryName,
            @PathVariable String secondCategoryName) {
        List<Product> res = productService.findByCategoryTopAndSecond(topCategoryName,secondCategoryName);
        List<ProductDTO> productDTOs = res.stream()
                .map(ProductDTO::new)
                .toList();
        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }

    @GetMapping("/{categoryName}")
    public ResponseEntity<List<ProductDTO>> findProductByCategory(
            @PathVariable String categoryName) {
        List<Product> products = productService.findProductByCategory(categoryName);
        List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::new)
                .toList();
        return new ResponseEntity<>(productDTOs, HttpStatus.OK);
    }

    @GetMapping("/search/{productName}")
    public ResponseEntity<ApiResponse> searchProducts(@PathVariable String productName) {
        List<Product> products = productService.searchProducts(productName);
        List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(productDTOs, "Search products successfully"));
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
