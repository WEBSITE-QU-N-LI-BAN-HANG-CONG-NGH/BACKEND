package com.webanhang.team_project.controller.admin;

import com.webanhang.team_project.dto.product.CreateProductRequest;
import com.webanhang.team_project.dto.product.ProductDTO;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/products")
public class AdminProductController {

    private final IProductService productService;
    private final CategoryRepository categoryRepository;

    /**
     * Tạo mới sản phẩm
     *
     * @param request DTO chứa thông tin sản phẩm cần tạo
     * @return Thông tin sản phẩm đã được tạo
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createProduct(@RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Tạo sản phẩm thành công"));
    }

    /**
     * Xóa sản phẩm theo ID
     *
     * @param productId ID của sản phẩm cần xóa
     * @return Thông báo kết quả xóa
     */
    @DeleteMapping("/{productId}/delete")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long productId) {
        String result = productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(null, result));
    }

    /**
     * Lấy danh sách tất cả sản phẩm
     *
     * @return Danh sách sản phẩm trong hệ thống
     */
    @Transactional
    @GetMapping("/all")
    public ResponseEntity<ApiResponse> findAllProducts() {
        List<Product> products = productService.findAllProducts();
//        List<ProductDTO> productDTOs = products.stream()
//                .map(ProductDTO::new)
//                .toList();
        return ResponseEntity.ok(ApiResponse.success(products, "Lấy tất cả sản phẩm thành công"));
    }

    /**
     * Cập nhật thông tin sản phẩm
     *
     * @param productId ID sản phẩm cần cập nhật
     * @param product Thông tin mới của sản phẩm
     * @return Thông tin sản phẩm sau khi cập nhật
     */
    @PutMapping("/{productId}/update")
    @Transactional
    public ResponseEntity<ApiResponse> updateProduct(@PathVariable Long productId, @RequestBody Product product) {
        Product updatedProduct = productService.updateProduct(productId, product);
        ProductDTO productDTO = new ProductDTO(updatedProduct);
        return ResponseEntity.ok(ApiResponse.success(productDTO, "Cập nhật sản phẩm thành công"));
    }

    /**
     * Tạo nhiều sản phẩm cùng lúc
     *
     * @param requests Mảng chứa thông tin các sản phẩm cần tạo
     * @return Thông báo kết quả thực hiện
     */
    @PostMapping("/create-multiple")
    public ResponseEntity<ApiResponse> createMultipleProducts(@RequestBody CreateProductRequest[] requests) {
        for (CreateProductRequest request : requests) {
            productService.createProduct(request);
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Tạo nhiều sản phẩm thành công"));
    }

    /**
     * Lấy danh sách sản phẩm bán chạy nhất
     *
     * @param limit Số lượng sản phẩm tối đa cần lấy (mặc định: 10)
     * @return Danh sách sản phẩm bán chạy
     */
    @GetMapping("/top-selling")
    public ResponseEntity<ApiResponse> getTopSellingProducts(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> topProducts = productService.getTopSellingProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(topProducts, "Get top selling products success"));
    }

    /**
     * Lấy thống kê doanh thu theo danh mục
     *
     * @return Dữ liệu doanh thu theo từng danh mục
     */
    @GetMapping("/revenue-by-category")
    public ResponseEntity<ApiResponse> getRevenueByCateogry() {
        Map<String, Object> categoryRevenue = productService.getRevenueByCateogry();
        return ResponseEntity.ok(ApiResponse.success(categoryRevenue, "Get revenue by category success"));
    }
}