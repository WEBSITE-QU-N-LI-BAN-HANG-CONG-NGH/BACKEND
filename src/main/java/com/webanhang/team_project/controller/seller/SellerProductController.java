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

    /**
     * Lấy danh sách sản phẩm của người bán với phân trang và tìm kiếm
     *
     * @param jwt Token xác thực người dùng
     * @param page Số trang (bắt đầu từ 0)
     * @param size Kích thước trang
     * @param search Từ khóa tìm kiếm
     * @return Danh sách sản phẩm đã phân trang và lọc
     */
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

    /**
     * Tạo sản phẩm mới
     *
     * @param jwt Token xác thực người dùng
     * @param request Thông tin sản phẩm cần tạo
     * @return Thông tin sản phẩm đã tạo
     */
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

    /**
     * Lấy chi tiết sản phẩm
     *
     * @param jwt Token xác thực người dùng
     * @param productId ID của sản phẩm cần xem
     * @return Thông tin chi tiết sản phẩm
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse> getProductDetails(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long productId) {

        User seller = userService.findUserByJwt(jwt);
        Product product = sellerProductService.getProductDetail(seller.getId(), productId);

        return ResponseEntity.ok(ApiResponse.success(product, "Lấy thông tin sản phẩm thành công"));
    }

    /**
     * Cập nhật thông tin sản phẩm
     *
     * @param jwt Token xác thực người dùng
     * @param productId ID của sản phẩm cần cập nhật
     * @param productRequest Thông tin mới của sản phẩm
     * @return Thông tin sản phẩm sau khi cập nhật
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse> updateProduct(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long productId,
            @RequestBody Product productRequest) {

        User seller = userService.findUserByJwt(jwt);
        Product updatedProduct = sellerProductService.updateProduct(seller.getId(), productId, productRequest);

        return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Cập nhật sản phẩm thành công"));
    }

    /**
     * Xóa sản phẩm
     *
     * @param jwt Token xác thực người dùng
     * @param productId ID của sản phẩm cần xóa
     * @return Thông báo kết quả
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse> deleteProduct(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long productId) {

        User seller = userService.findUserByJwt(jwt);
        sellerProductService.deleteProduct(seller.getId(), productId);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa sản phẩm thành công"));
    }

    /**
     * Cập nhật số lượng tồn kho của sản phẩm
     *
     * @param jwt Token xác thực người dùng
     * @param productId ID của sản phẩm cần cập nhật
     * @param quantity Số lượng mới
     * @return Thông tin sản phẩm sau khi cập nhật
     */
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
