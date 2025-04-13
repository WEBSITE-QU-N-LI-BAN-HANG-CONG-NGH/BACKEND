package com.webanhang.team_project.controller.admin;

import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/products")
public class AdminProductController {

    private final IProductService productService;
    private final CategoryRepository categoryRepository;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createProduct(@RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Tạo sản phẩm thành công"));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long productId) {
        String result = productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(null, result));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> findAllProducts() {
        List<Product> products = productService.findAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products, "Lấy tất cả sản phẩm thành công"));
    }

    @PutMapping("/{productId}/update")
    public ResponseEntity<ApiResponse> updateProduct(@PathVariable Long productId, @RequestBody Product product) {
        Product updatedProduct = productService.updateProduct(productId, product);
        return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Cập nhật sản phẩm thành công"));
    }

    @PostMapping("/create-multiple")
    public ResponseEntity<ApiResponse> createMultipleProducts(@RequestBody CreateProductRequest[] requests) {
        for (CreateProductRequest request : requests) {
            productService.createProduct(request);
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Tạo nhiều sản phẩm thành công"));
    }

    @GetMapping("/top-selling")
    public ResponseEntity<ApiResponse> getTopSellingProducts(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> topProducts = productService.getTopSellingProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(topProducts, "Get top selling products success"));
    }

    @GetMapping("/revenue-by-category")
    public ResponseEntity<ApiResponse> getRevenueByCateogry() {
        Map<String, Object> categoryRevenue = productService.getRevenueByCateogry();
        return ResponseEntity.ok(ApiResponse.success(categoryRevenue, "Get revenue by category success"));
    }
}