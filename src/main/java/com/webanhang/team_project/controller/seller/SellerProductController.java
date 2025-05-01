package com.webanhang.team_project.controller.seller;

import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.service.product.IProductService;
import com.webanhang.team_project.service.seller.ISellerProductService;
import com.webanhang.team_project.service.seller.SellerProductService;
import com.webanhang.team_project.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/seller/products")
public class SellerProductController {

    private final ISellerProductService sellerProductService;
    private final IUserService userService;

    /**
     * Tạo mới sản phẩm cho người bán
     *
     * @param request DTO chứa thông tin sản phẩm cần tạo
     * @param jwt Token xác thực của người bán
     * @return Thông tin sản phẩm đã được tạo
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createProduct(
            @RequestBody CreateProductRequest req,
            @RequestHeader("Authorization") String jwt) {

        User seller = userService.findUserByJwt(jwt);
        req.setSellerId(seller.getId());
        ProductDTO productDto = sellerProductService.createProduct(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(productDto, "Tạo sản phẩm thành công"));
    }

    /**
     * Cập nhật thông tin sản phẩm của người bán
     *
     * @param productId ID sản phẩm cần cập nhật
     * @param updatedProduct Thông tin mới của sản phẩm
     * @param jwt Token xác thực của người bán
     * @return Thông tin sản phẩm sau khi cập nhật
     */
    @PutMapping("/{productId}/update")
    public ResponseEntity<ApiResponse> updateProduct(
            @PathVariable Long productId,
            @RequestBody Product updatedProduct,
            @RequestHeader("Authorization") String jwt) {

        ProductDTO productDto = sellerProductService.updateProduct(productId, updatedProduct);

        return ResponseEntity.ok(ApiResponse.success(productDto, "Cập nhật sản phẩm thành công"));
    }

    /**
     * Xóa sản phẩm
     *
     * @param jwt Token xác thực người dùng
     * @param productId ID của sản phẩm cần xóa
     * @return Thông báo kết quả
     */
    @DeleteMapping("/{productId}/delete")
    public ResponseEntity<ApiResponse> deleteProduct(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long productId) {

        sellerProductService.deleteProduct(productId);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa sản phẩm thành công"));
    }

    /**
     * Lấy danh sách sản phẩm của người bán
     *
     * @param jwt Token xác thực của người bán
     * @return Danh sách sản phẩm của người bán
     */
    @GetMapping("/list-products")
    public ResponseEntity<ApiResponse> getSellerProducts(@RequestHeader("Authorization") String jwt) {

        User seller = userService.findUserByJwt(jwt);
        List<ProductDTO> productDTOs = sellerProductService.getSellerProducts(seller.getId());

        return ResponseEntity
                .ok(ApiResponse.success(productDTOs, "Lấy danh sách sản phẩm thành công"));
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

        ProductDTO productDTO = sellerProductService.getProductDetail(productId);

        return ResponseEntity.ok(ApiResponse.success(productDTO, "Lấy chi tiết sản phẩm thành công"));
    }

    /**
     * Lấy thống kê về sản phẩm của người bán
     *
     * @param jwt Token xác thực của người bán
     * @return Thống kê sản phẩm
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getProductStats(@RequestHeader("Authorization") String jwt) {

        User seller = userService.findUserByJwt(jwt);
        Map<String, Object> stats = sellerProductService.getProductStatOfSeller(seller.getId());

        return ResponseEntity
                .ok(ApiResponse.success(stats, "Lấy thống kê sản phẩm thành công"));
    }

    /**
     * Tạo hàng loạt sản phẩm -> only dev testing
     *
     * @param requests List DTO chứa thông tin sản phẩm cần tạo
     * @param jwt Token xác thực của người bán
     * @return Tạo hàng loạt sản phẩm
     */
    @PostMapping("/create-multi-product")
    public ResponseEntity<ApiResponse> createMultipleProduct(
            @RequestBody List<CreateProductRequest> requests,
            @RequestHeader("Authorization") String jwt) {

        List<ProductDTO> dtos = sellerProductService.createMultipleProducts(requests);

        return ResponseEntity
                .ok(ApiResponse.success(dtos, "Tạo nhiều sản phẩm thành công"));
    }
}
