package com.webanhang.team_project.controller.seller;

import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.product.IProductService;
import com.webanhang.team_project.service.seller.ISellerProductService;
import com.webanhang.team_project.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/seller/products")
public class SellerProductController {

    private final ISellerProductService sellerProductService;
    private final IProductService productService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse> getSellerProducts(
            @RequestHeader("Authorization") String jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        User seller = userService.findUserByJwt(jwt);
        Page<Product> products = sellerProductService.getSellerProducts(seller.getId(), page, size, search);

        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent());
        response.put("currentPage", products.getNumber());
        response.put("totalItems", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách sản phẩm thành công"));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createProduct(
            @RequestHeader("Authorization") String jwt,
            @RequestBody CreateProductRequest request) {

        User seller = userService.findUserByJwt(jwt);
        Product product = sellerProductService.createProduct(seller.getId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Tạo sản phẩm thành công"));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse> getProductDetails(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long productId) {

        User seller = userService.findUserByJwt(jwt);
        Product product = sellerProductService.getProductDetail(seller.getId(), productId);

        return ResponseEntity.ok(ApiResponse.success(product, "Lấy thông tin sản phẩm thành công"));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse> updateProduct(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long productId,
            @RequestBody Product productRequest) {

        User seller = userService.findUserByJwt(jwt);
        Product updatedProduct = sellerProductService.updateProduct(seller.getId(), productId, productRequest);

        return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Cập nhật sản phẩm thành công"));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse> deleteProduct(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long productId) {

        User seller = userService.findUserByJwt(jwt);
        sellerProductService.deleteProduct(seller.getId(), productId);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa sản phẩm thành công"));
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<ApiResponse> updateInventory(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long productId,
            @RequestParam int quantity) {

        User seller = userService.findUserByJwt(jwt);
        Product product = sellerProductService.updateInventory(seller.getId(), productId, quantity);

        return ResponseEntity.ok(ApiResponse.success(product, "Cập nhật tồn kho thành công"));
    }
}
